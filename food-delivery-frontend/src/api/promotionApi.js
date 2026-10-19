import api from "./axios";
export const quoteCoupon = (code, subtotal) => api.get(`/coupons/${encodeURIComponent(code)}/quote`, { params: { subtotal } });
