import { useMemo, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";

import {
  CardElement,
  Elements,
  useStripe,
  useElements,
} from "@stripe/react-stripe-js";

import { loadStripe } from "@stripe/stripe-js";

import MainLayout from "../layouts/MainLayout";
import Button from "../components/ui/Button";

import { verifyPayment } from "../api/paymentApi";
import { clearCart } from "../api/cartApi";
import { getOrderById } from "../api/orderApi";

import { clearCartState } from "../redux/slices/cartSlice";

const cardOptions = {
  style: {
    base: {
      fontSize: "16px",
      color: "#1f2937",
    },
    invalid: {
      color: "#dc2626",
    },
  },
};

const POLLING_INTERVAL = 1000;
const MAX_POLLING_ATTEMPTS = 30;

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// Wait until Payment Service receives the Stripe webhook
// and updates the payment status in its database.
const waitForPaymentSuccess = async (paymentIntentId) => {
  for (let attempt = 1; attempt <= MAX_POLLING_ATTEMPTS; attempt++) {
    const { data } = await verifyPayment(paymentIntentId);

    if (data.status === "SUCCESS") {
      return;
    }

    if (data.status === "FAILED") {
      throw new Error("Payment failed.");
    }

    if (attempt < MAX_POLLING_ATTEMPTS) {
      await delay(POLLING_INTERVAL);
    }
  }

  throw new Error(
    "Payment is still processing. Please check your order status shortly.",
  );
};

// Wait until Order Service consumes the Kafka event
// and changes the order status to CONFIRMED.
// const waitForOrderConfirmation = async (orderId) => {
//   for (
//     let attempt = 1;
//     attempt <= MAX_POLLING_ATTEMPTS;
//     attempt++
//   ) {
//     const { data } = await getOrderById(orderId);

//     if (data.status === "CONFIRMED") {
//       return;
//     }

//     if (data.status === "CANCELLED") {
//       throw new Error("Your order was cancelled.");
//     }

//     if (attempt < MAX_POLLING_ATTEMPTS) {
//       await delay(POLLING_INTERVAL);
//     }
//   }

//   throw new Error(
//     "Payment succeeded, but order confirmation is still processing. Please check your orders shortly."
//   );
// };

const waitForOrderConfirmation = async (orderId) => {
  for (let attempt = 1; attempt <= MAX_POLLING_ATTEMPTS; attempt++) {
    const { data } = await getOrderById(orderId);

    if (data.status === "CONFIRMED") {
      return;
    }

    if (data.status === "CANCELLED") {
      throw new Error("Your order was cancelled.");
    }

    if (attempt < MAX_POLLING_ATTEMPTS) {
      await delay(POLLING_INTERVAL);
    }
  }

  throw new Error(
    "Payment succeeded, but order confirmation is still processing. Please check your orders shortly.",
  );
};

function PaymentForm({ amount, orderId, clientSecret }) {
  const stripe = useStripe();
  const elements = useElements();

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [paymentStage, setPaymentStage] = useState("");

  const handlePay = async (event) => {
    event.preventDefault();

    if (!stripe || !elements || isSubmitting) {
      return;
    }

    setError("");
    setIsSubmitting(true);
    setPaymentStage("Connecting to Stripe...");

    try {
      // Step 1: Confirm payment with Stripe.
      const result = await stripe.confirmCardPayment(clientSecret, {
        payment_method: {
          card: elements.getElement(CardElement),
        },
      });

      if (result.error) {
        console.log("error came");
        
        throw new Error(result.error.message);
      }

      if (result.paymentIntent.status !== "succeeded") {
        throw new Error(
          `Payment is not completed. Current status: ${result.paymentIntent.status}`,
        );
      }

      const paymentIntentId = result.paymentIntent.id;

      // Step 2: Wait for Payment Service webhook processing.
      setPaymentStage("Verifying payment...");

      await waitForPaymentSuccess(paymentIntentId);

      // Step 3: Wait for Kafka consumer to confirm the order.
      setPaymentStage("Confirming your order...");

      await waitForOrderConfirmation(orderId);

      // Step 4: Clear cart only after order confirmation.
      setPaymentStage("Finalizing your order...");

      await clearCart();

      dispatch(clearCartState());

      sessionStorage.removeItem("checkoutIdempotencyKey");

      // Step 5: Navigate to success page.
      navigate("/order-success", {
        state: {
          orderId,
        },
      });
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          requestError.message ||
          "Payment could not be completed.",
      );
    } finally {
      setIsSubmitting(false);
      setPaymentStage("");
    }
  };

  return (
    <form onSubmit={handlePay} className="space-y-5">
      <div className="rounded-xl border border-gray-200 p-4">
        <CardElement options={cardOptions} />
      </div>

      <p className="text-sm text-gray-500">
        Test card: 4242 4242 4242 4242, any future date, any CVC.
      </p>

      {error && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</p>
      )}

      {paymentStage && <p className="text-sm text-blue-600">{paymentStage}</p>}

      <Button
        type="submit"
        className="w-full py-3.5"
        disabled={!stripe || !elements || isSubmitting}
      >
        {isSubmitting ? "Processing payment..." : `Pay ₹${amount}`}
      </Button>
    </form>
  );
}

export default function PaymentPage() {
  const { state } = useLocation();

  const stripePromise = useMemo(() => {
    const key = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;

    return key ? loadStripe(key) : null;
  }, []);

  const amount = state?.amount ?? 0;
  const orderId = state?.orderId;
  const clientSecret = state?.clientSecret;

  if (!orderId || !clientSecret) {
    return <Navigate to="/checkout" replace />;
  }

  return (
    <MainLayout>
      <div className="mx-auto max-w-md p-6">
        <h1 className="mb-6 text-3xl font-bold">Payment</h1>

        <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-md">
          <h2 className="mb-6 text-xl font-bold">Cart subtotal: ₹{amount}</h2>

          {stripePromise ? (
            <Elements stripe={stripePromise}>
              <PaymentForm
                amount={amount}
                orderId={orderId}
                clientSecret={clientSecret}
              />
            </Elements>
          ) : (
            <p className="rounded-lg bg-red-50 p-3 text-red-600">
              Set VITE_STRIPE_PUBLISHABLE_KEY to your Stripe test publishable
              key.
            </p>
          )}
        </div>
      </div>
    </MainLayout>
  );
}
