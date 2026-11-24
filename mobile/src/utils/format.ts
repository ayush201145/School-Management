export const formatCurrency = (val: any): string => {
  const num = Number(val);
  return (isNaN(num) ? 0 : num).toFixed(2);
};

export const toNum = (val: any): number => {
  const num = Number(val);
  return isNaN(num) ? 0 : num;
};
