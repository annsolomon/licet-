import { api } from '../services/api.js';
import { inr } from '../utils/format.js';

const opts = (url, label) => async () => (await api.get(url)).map((r) => ({ value: r.id, label: label(r) }));
const departments = opts('/api/departments', (d) => `${d.code} - ${d.name}`);

export const departmentConfig = {
  title: 'Departments', subtitle: 'Academic departments', endpoint: '/api/departments',
  flow: ['React page', 'GET/POST /api/departments', 'DepartmentController', 'DepartmentService (validation)', 'DepartmentRepository', 'JdbcTemplate', 'SELECT / INSERT ... department', 'Oracle'],
  columns: [{ key: 'id', label: 'ID' }, { key: 'code', label: 'Code' }, { key: 'name', label: 'Name' }, { key: 'studentCount', label: 'Students' }],
  fields: [{ name: 'code', label: 'Code (e.g. CSE)' }, { name: 'name', label: 'Name' }],
  toForm: (r) => ({ code: r.code, name: r.name }),
};

export const facultyConfig = {
  title: 'Faculty', subtitle: 'Teaching staff (Faculty extends Person)', endpoint: '/api/faculty',
  flow: ['React page', '/api/faculty', 'FacultyController', 'FacultyService', 'FacultyRepository + RowMapper', 'JdbcTemplate', 'faculty table', 'Oracle'],
  columns: [{ key: 'id', label: 'ID' }, { key: 'code', label: 'Code' }, { key: 'name', label: 'Name' }, { key: 'email', label: 'Email' },
    { key: 'designation', label: 'Designation' }, { key: 'deptCode', label: 'Dept' }],
  fields: [{ name: 'code', label: 'Code (e.g. F010)' }, { name: 'name', label: 'Name' }, { name: 'email', label: 'Email', type: 'email' },
    { name: 'designation', label: 'Designation' }, { name: 'departmentId', label: 'Department', type: 'select', options: departments }],
  toForm: (r) => ({ code: r.code, name: r.name, email: r.email, designation: r.designation, departmentId: r.deptId }),
};

export const subjectConfig = {
  title: 'Subjects', subtitle: 'Courses with credits and semester', endpoint: '/api/subjects',
  flow: ['React page', '/api/subjects', 'SubjectController', 'SubjectService (code format, credits 1-6)', 'SubjectRepository', 'JdbcTemplate', 'subject table', 'Oracle'],
  columns: [{ key: 'id', label: 'ID' }, { key: 'code', label: 'Code' }, { key: 'name', label: 'Name' }, { key: 'credits', label: 'Credits' },
    { key: 'semester', label: 'Sem' }, { key: 'deptCode', label: 'Dept' }],
  fields: [{ name: 'code', label: 'Code (e.g. CS305)' }, { name: 'name', label: 'Name' }, { name: 'credits', label: 'Credits', type: 'number' },
    { name: 'semester', label: 'Semester', type: 'number' }, { name: 'departmentId', label: 'Department', type: 'select', options: departments }],
  toForm: (r) => ({ code: r.code, name: r.name, credits: r.credits, semester: r.semester, departmentId: r.deptId }),
};

export const classConfig = {
  title: 'Classes', subtitle: 'Sections (department + semester + section)', endpoint: '/api/classes',
  flow: ['React page', '/api/classes', 'ClassController', 'ClassService', 'ClassRepository', 'JdbcTemplate', 'class / class_subject', 'Oracle'],
  columns: [{ key: 'id', label: 'ID' }, { key: 'name', label: 'Name' }, { key: 'deptCode', label: 'Dept' }, { key: 'semester', label: 'Sem' },
    { key: 'section', label: 'Section' }, { key: 'academicYear', label: 'Year' }, { key: 'studentCount', label: 'Students' }],
  fields: [{ name: 'name', label: 'Name (e.g. CSE-III-B)' }, { name: 'departmentId', label: 'Department', type: 'select', options: departments },
    { name: 'semester', label: 'Semester', type: 'number' }, { name: 'section', label: 'Section' }, { name: 'academicYear', label: 'Academic year (2026-2027)' }],
  toForm: (r) => ({ name: r.name, departmentId: r.deptId, semester: r.semester, section: r.section, academicYear: r.academicYear }),
};

export const employeeConfig = {
  title: 'Employees', subtitle: 'Staff for payroll (Employee -> Programmer / Professor ...)', endpoint: '/api/employees',
  flow: ['React page', '/api/employees', 'EmployeeController', 'EmployeeService (Designation enum)', 'EmployeeRepository', 'JdbcTemplate', 'employee table', 'Oracle'],
  columns: [{ key: 'id', label: 'ID' }, { key: 'name', label: 'Name' }, { key: 'email', label: 'Email' }, { key: 'designation', label: 'Designation' },
    { key: 'basicSalary', label: 'Basic salary', render: (r) => inr(r.basicSalary) }],
  fields: [{ name: 'name', label: 'Name' }, { name: 'email', label: 'Email', type: 'email' },
    { name: 'designation', label: 'Designation (PROGRAMMER, ASSISTANT_PROFESSOR, ASSOCIATE_PROFESSOR, PROFESSOR)' },
    { name: 'basicSalary', label: 'Basic salary', type: 'number' }],
  toForm: (r) => ({ name: r.name, email: r.email, designation: r.designation, basicSalary: r.basicSalary }),
};

export const studentFields = [
  { name: 'id', label: 'Student ID (blank = automatic)', type: 'number', readOnlyOnEdit: true },
  { name: 'name', label: 'Name' }, { name: 'email', label: 'Email', type: 'email' }, { name: 'phone', label: 'Phone' },
  { name: 'departmentId', label: 'Department', type: 'select', options: departments },
  { name: 'classId', label: 'Class', type: 'select', options: opts('/api/classes', (c) => c.name) },
  { name: 'joinedYear', label: 'Joined year', type: 'number' },
];
export const studentBase = {
  title: 'Students', subtitle: 'Student is a Person (inheritance); stored in table STUDENT', endpoint: '/api/students',
  flow: ['React page', '/api/students', 'StudentController', 'StudentService (Java validation + GenericSearch)', 'StudentRepository', 'JdbcTemplate + RowMapper', 'SELECT ... student JOIN department JOIN class', 'Oracle (PK, UNIQUE, FK, CHECK, triggers)'],
  columns: [{ key: 'id', label: 'ID' }, { key: 'name', label: 'Name' }, { key: 'email', label: 'Email' }, { key: 'phone', label: 'Phone' },
    { key: 'deptCode', label: 'Dept' }, { key: 'className', label: 'Class' }, { key: 'joinedYear', label: 'Joined' }],
  fields: studentFields,
  toForm: (r) => ({ id: r.id, name: r.name, email: r.email, phone: r.phone ?? '', departmentId: r.deptId, classId: r.classId, joinedYear: r.joinedYear ?? '' }),
};
