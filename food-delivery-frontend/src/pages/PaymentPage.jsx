import { useMemo, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { CardElement, Elements, useStripe, useElements } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";

import MainLayout from "../layouts/MainLayout";
import Button from "../components/ui/Button";
import { confirmOrder, placeOrder } from "../api/orderApi";
import { verifyPayment } from "../api/paymentApi";
import { clearCart } from "../api/cartApi";
import { clearCartState } from "../redux/slices/cartSlice";

const cardOptions = { style: { base: { fontSize: "16px", color: "#1f2937" }, invalid: { color: "#dc2626" } } };

function PaymentForm({ amount, deliveryAddress, couponCode, coordinates, customerPhone, smsOptIn }) {
  const stripe = useStripe();
  const elements = useElements();
  const dispatch = useDispatch();
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [finalAmount, setFinalAmount] = useState(amount);
  const navigate = useNavigate();

  const handlePay = async (event) => {
    event.preventDefault();
    if (!stripe || !elements) return;
    setError("");
    setIsSubmitting(true);
    try {
      const { data: payment } = await placeOrder(deliveryAddress, couponCode, coordinates, customerPhone, smsOptIn);
      setFinalAmount(payment.total ?? amount);
      const result = await stripe.confirmCardPayment(payment.clientSecret, {
        payment_method: { card: elements.getElement(CardElement) },
      });
      if (result.error) throw new Error(result.error.message);
      await verifyPayment(result.paymentIntent.id);
      await confirmOrder(payment.orderId);
      await clearCart();
      sessionStorage.removeItem("checkoutIdempotencyKey");
      dispatch(clearCartState());
      navigate("/order-success", { state: { orderId: payment.orderId } });
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || "Payment could not be completed.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return <form onSubmit={handlePay} className="space-y-5">
    <div className="rounded-xl border border-gray-200 p-4"><CardElement options={cardOptions} /></div>
    <p className="text-sm text-gray-500">Test card: 4242 4242 4242 4242, any future date, any CVC.</p>
    {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</p>}
    <Button type="submit" className="w-full py-3.5" disabled={!stripe || isSubmitting}>
      {isSubmitting ? "Processing payment..." : `Pay ₹${finalAmount}`}
    </Button>
  </form>;
}

export default function PaymentPage() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const stripePromise = useMemo(() => {
    const key = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
    return key ? loadStripe(key) : null;
  }, []);
  const amount = state?.amount || 0;

  if (!state?.deliveryAddress) {
    return <Navigate to="/checkout" replace />;
  }
  return <MainLayout><div className="mx-auto max-w-md p-6"><h1 className="mb-6 text-3xl font-bold">Payment</h1>
      <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-md"><h2 className="mb-6 text-xl font-bold">Cart subtotal: ₹{amount}</h2>
      {stripePromise ? <Elements stripe={stripePromise}><PaymentForm amount={amount} deliveryAddress={state.deliveryAddress} couponCode={state.couponCode} coordinates={state.coordinates} customerPhone={state.customerPhone} smsOptIn={state.smsOptIn} /></Elements> :
        <p className="rounded-lg bg-red-50 p-3 text-red-600">Set VITE_STRIPE_PUBLISHABLE_KEY to your Stripe test publishable key.</p>}
    </div></div></MainLayout>;
}
