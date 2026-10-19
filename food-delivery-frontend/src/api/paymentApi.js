import api from "./axios";

export const verifyPayment = (paymentIntentId) =>
  api.post("/payments/verify", { paymentIntentId });
