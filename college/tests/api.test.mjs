// End-to-end API tests: they call the RUNNING backend (./run.sh) and check the real Oracle results.
// Run:  node --test tests/        (or ./test.sh)       Needs Node 20+ (built-in fetch and node:test).
import test from 'node:test';
import assert from 'node:assert/strict';

const API = process.env.API_URL || 'http://localhost:8080';

async function call(method, path, { body, token } = {}) {
  const res = await fetch(API + path, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  return { status: res.status, body: json };
}
const login = async (u, p) => (await call('POST', '/api/auth/login', { body: { username: u, password: p } }));
const near = (a, b, d = 0.011) => assert.ok(Math.abs(Number(a) - b) <= d, `expected ${a} to be near ${b}`);

let admin, faculty;
test.before(async () => {
  admin = (await login('admin', 'admin123')).body.token;
  faculty = (await login('faculty', 'faculty123')).body.token;
});

test('health: API and Oracle are up', async () => {
  const r = await call('GET', '/api/health');
  assert.equal(r.status, 200);
  assert.equal(r.body.database, 'UP');
  assert.ok(r.body.tables >= 17);
});

test('login: wrong password -> 401, no token -> 401, faculty on admin page -> 403', async () => {
  assert.equal((await login('admin', 'wrong')).status, 401);
  assert.equal((await call('GET', '/api/students')).status, 401);
  assert.equal((await call('GET', '/api/employees', { token: faculty })).status, 403);
  assert.equal((await call('GET', '/api/employees', { token: admin })).status, 200);
});

test('dashboard and master data are loaded', async () => {
  const d = (await call('GET', '/api/dashboard', { token: admin })).body;
  assert.ok(d.students >= 10 && d.faculty >= 6 && d.departments >= 4);
  const depts = (await call('GET', '/api/departments', { token: admin })).body;
  assert.ok(depts.some((x) => x.code === 'CSE'));
});

test('student validation: Java rejects a bad e-mail (400, layer = Java validation)', async () => {
  const r = await call('POST', '/api/students', { token: admin, body: { name: 'Bad Mail', email: 'nope', departmentId: 1, classId: 1 } });
  assert.equal(r.status, 400);
  assert.equal(r.body.layer, 'Java validation');
});

test('student CRUD, duplicate e-mail by Java (409) and by Oracle constraint (409)', async () => {
  const body = { name: 'Test Student', email: 'test.student@college.edu', departmentId: 1, classId: 1, joinedYear: 2026 };
  const created = await call('POST', '/api/students', { token: admin, body });
  assert.equal(created.status, 201);
  const id = created.body.id;
  const dupJava = await call('POST', '/api/students', { token: admin, body });
  assert.equal(dupJava.status, 409);
  const dupOracle = await call('POST', '/api/students?skipChecks=true', { token: admin, body });
  assert.equal(dupOracle.status, 409);
  assert.equal(dupOracle.body.layer, 'Oracle constraint');
  assert.equal((await call('DELETE', `/api/students/${id}`, { token: admin })).status, 204);
  assert.equal((await call('GET', `/api/students/${id}`, { token: admin })).status, 404);
  // the audit triggers recorded the insert and the delete
  const audit = (await call('GET', '/api/audit?limit=20', { token: admin })).body;
  assert.ok(audit.some((a) => a.tableName === 'STUDENT' && a.operation === 'INSERT'));
  assert.ok(audit.some((a) => a.tableName === 'STUDENT' && a.operation === 'DELETE'));
});

test('department with students cannot be deleted (ORA-02292 -> 409)', async () => {
  const r = await call('DELETE', '/api/departments/1', { token: admin });
  assert.equal(r.status, 409);
});

test('MARK FORMULA: Rahul / CS302 -> internal 33.4, semester 49.2, final 82.6 (Java = Oracle)', async () => {
  const b = (await call('GET', '/api/marks/breakdown?studentId=101&subjectId=2', { token: admin })).body;
  assert.equal(b.complete, true);
  near(b.javaCalc.internal, 33.4);
  near(b.javaCalc.semester, 49.2);
  near(b.javaCalc.finalMark, 82.6);
  near(b.oracleFinal, 82.6);
  assert.equal(b.match, true);
});

test('marks above maximum: Java 400; with skipValidation the Oracle TRIGGER rejects (400, layer Trigger)', async () => {
  const body = { studentId: 101, subjectId: 2, assessmentCode: 'CT1', rawMarks: 99 };
  const java = await call('POST', '/api/marks', { token: admin, body });
  assert.equal(java.status, 400);
  const trig = await call('POST', '/api/marks?skipValidation=true', { token: admin, body });
  assert.equal(trig.status, 400);
  assert.equal(trig.body.layer, 'Trigger');
});

test('results: PL/SQL calculates Rahul CS302 = 82.6, grade A+, pass; CGPA from Oracle equals Java', async () => {
  const all = await call('POST', '/api/results/calculate-all', { token: faculty });
  assert.equal(all.status, 200);
  const s = (await call('GET', '/api/results/student/101', { token: admin })).body;
  const cs302 = s.results.find((r) => r.subjectCode === 'CS302');
  near(cs302.finalMark, 82.6);
  assert.equal(cs302.grade, 'A+');
  assert.equal(cs302.passFail, 'PASS');
  near(s.cgpa, Number(s.javaCgpa));
});

test('grade scale is configurable data', async () => {
  const g = (await call('GET', '/api/grades', { token: admin })).body;
  assert.ok(g.some((x) => x.code === 'O') && g.some((x) => x.code === 'RA'));
});

test('attendance: Divya (104) is in the low-attendance report', async () => {
  const low = (await call('GET', '/api/attendance/low', { token: admin })).body;
  assert.ok(low.some((x) => x.studentId === 104));
  assert.ok(low.every((x) => Number(x.percentage) < 75));
});

test('payroll: Professor basic 80000 -> gross 104000, net 94400 (Java and PL/SQL agree, duplicate blocked)', async () => {
  const month = '2031-01';
  await call('DELETE', `/api/payroll?month=${month}`, { token: admin });
  const java = await call('POST', '/api/payroll/generate', { token: admin, body: { empId: 1001, month } });
  assert.equal(java.status, 200);
  near(java.body.gross, 104000, 0.01);
  near(java.body.net, 94400, 0.01);
  const dup = await call('POST', '/api/payroll/generate', { token: admin, body: { empId: 1001, month } });
  assert.equal(dup.status, 409);
  const plsql = await call('POST', '/api/payroll/generate-plsql', { token: admin, body: { empId: 1002, month } });
  assert.equal(plsql.status, 200);
  const javaCheck = await call('POST', '/api/payroll/generate', { token: admin, body: { empId: 1003, month } });
  const plsqlCheck = await call('POST', '/api/payroll/generate-plsql', { token: admin, body: { empId: 1004, month } });
  assert.equal(javaCheck.status, 200);
  assert.equal(plsqlCheck.status, 200);
  const slip = (await call('GET', `/api/payroll/${java.body.id}/payslip`, { token: admin })).body;
  assert.ok(slip.lines.join('\n').includes('94400'));
  const del = await call('DELETE', `/api/payroll?month=${month}`, { token: admin });
  assert.equal(del.status, 200);
});

test('reports: generate a file, count a character, copy it', async () => {
  const gen = await call('POST', '/api/reports', { token: faculty, body: { type: 'STUDENT' } });
  assert.equal(gen.status, 200);
  assert.ok(gen.body.characters > 0);
  const cnt = await call('GET', `/api/reports/files/${gen.body.name}/count?character=a`, { token: faculty });
  assert.equal(cnt.status, 200);
  assert.ok(cnt.body.occurrences > 0);
  assert.equal((await call('POST', `/api/reports/files/${gen.body.name}/copy`, { token: faculty })).status, 200);
  const par = await call('POST', '/api/reports/parallel', { token: faculty, body: { types: ['STUDENT', 'ATTENDANCE', 'RESULT'] } });
  assert.equal(par.body.length, 3);
});

test('path traversal in report names is rejected', async () => {
  const r = await call('GET', '/api/reports/files/..%2F..%2Fetc%2Fpasswd', { token: admin });
  assert.ok(r.status === 400 || r.status === 404);
});

test('Java lab runs all syllabus demos', async () => {
  const all = (await call('GET', '/api/lab/all', { token: admin })).body;
  assert.ok(Object.keys(all).length >= 15);
  assert.equal((await call('GET', '/api/lab/factorial?n=5', { token: admin })).body.iterative, 120);
});

test('DBMS lab: 32 topics and every demo runs without an unexpected error', async () => {
  const topics = (await call('GET', '/api/dblab/topics', { token: admin })).body;
  assert.equal(topics.length, 32);
  for (const t of topics) {
    const steps = (await call('POST', `/api/dblab/topics/${t.number}/run`, { token: admin })).body;
    const bad = steps.filter((s) => s.error);
    assert.deepEqual(bad.map((s) => `${t.number} ${t.title}: ${s.message}`), [], `topic ${t.number} failed`);
  }
});

test('demo data is untouched after the DBMS lab (everything rolled back)', async () => {
  const b = (await call('GET', '/api/marks/breakdown?studentId=101&subjectId=2', { token: admin })).body;
  near(b.javaCalc.finalMark, 82.6);
  const d = (await call('GET', '/api/departments', { token: admin })).body;
  assert.ok(!d.some((x) => x.code === 'DEMO' || x.code === 'TXN'));
});
