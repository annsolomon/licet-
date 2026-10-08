export const num = (v, d = 2) => (v === null || v === undefined ? '-' : Number(v).toFixed(d));
export const inr = (v) => (v === null || v === undefined ? '-' : Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
export const today = () => new Date().toISOString().slice(0, 10);
export const thisMonth = () => new Date().toISOString().slice(0, 7);
