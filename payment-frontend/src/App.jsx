
import { useState } from "react";
import { loadStripe } from "@stripe/stripe-js";
import {
  Elements,
  PaymentElement,
  useElements,
  useStripe,
} from "@stripe/react-stripe-js";

const stripePromise = loadStripe(
  import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY
);

const API_URL =
  import.meta.env.VITE_PAYMENT_API_URL || "http://localhost:8081";

function CheckoutForm({ paymentId }) {
  const stripe = useStripe();
  const elements = useElements();

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();

    if (!stripe || !elements) {
      setMessage("Stripe is still loading. Please wait.");
      return;
    }

    setLoading(true);
    setMessage("");

    try {
      const { error: submitError } = await elements.submit();

      if (submitError) {
        setMessage(submitError.message);
        return;
      }

      const { error, paymentIntent } = await stripe.confirmPayment({
        elements,
        redirect: "if_required",
        confirmParams: {
          return_url:
            `${window.location.origin}/?paymentId=${paymentId}`,
        },
      });

      if (error) {
        setMessage(error.message || "Payment could not be completed.");
      } else if (paymentIntent?.status === "succeeded") {
        setMessage(
          "Stripe confirms payment success. Check MySQL after the webhook is processed."
        );
      } else {
        setMessage(
          `Payment status: ${paymentIntent?.status || "processing"}`
        );
      }
    } catch (error) {
      setMessage(error.message || "Unexpected payment error.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <PaymentElement />

      <button
        type="submit"
        disabled={!stripe || !elements || loading}
      >
        {loading ? "Processing..." : "Pay now"}
      </button>

      {message && <p role="status">{message}</p>}
    </form>
  );
}

export default function App() {
  const queryPaymentId = new URLSearchParams(
    window.location.search
  ).get("paymentId");

  const [paymentId, setPaymentId] = useState(queryPaymentId || "");
  const [clientSecret, setClientSecret] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function loadPayment(event) {
    event.preventDefault();

    setClientSecret("");
    setError("");

    if (!paymentId || !Number.isSafeInteger(Number(paymentId))
        || Number(paymentId) <= 0) {
      setError("Enter a valid payment ID.");
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(
        `${API_URL}/payments/${encodeURIComponent(paymentId)}/client-secret`
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || data.error || "Could not load payment.");
      }

      if (!data.clientSecret) {
        throw new Error("The backend did not return a client secret.");
      }

      setClientSecret(data.clientSecret);
    } catch (error) {
      setError(
        `${error.message}. Check that Payment Service is running and the API endpoint exists.`
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="checkout">
      <h1>Test Checkout</h1>
      <p>Complete a Stripe test payment for an existing payment record.</p>

      <form onSubmit={loadPayment}>
        <label htmlFor="paymentId">MySQL Payment ID</label>

        <input
          id="paymentId"
          type="number"
          min="1"
          value={paymentId}
          onChange={(event) => setPaymentId(event.target.value)}
          placeholder="For example, 11"
          required
        />

        <button type="submit" disabled={loading}>
          {loading ? "Loading payment..." : "Load payment"}
        </button>
      </form>

      {error && <p role="alert">{error}</p>}

      {clientSecret && (
        <section>
          <h2>Payment details</h2>

          <Elements
            stripe={stripePromise}
            options={{ clientSecret }}
          >
            <CheckoutForm paymentId={paymentId} />
          </Elements>
        </section>
      )}
    </main>
  );
}
