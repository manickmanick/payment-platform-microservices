
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";
import {
  cancelOrder,
  getMyOrders,
  resumeOrderPayment,
} from "../api/orderApi";

import DriverTrackingMap from "../components/order/DriverTrackingMap";

const statusColor = {
  PLACED: "bg-yellow-100 text-yellow-700",
  PENDING: "bg-yellow-100 text-yellow-700",
  CONFIRMED: "bg-blue-100 text-blue-700",
  ACCEPTED: "bg-blue-100 text-blue-700",
  PREPARING: "bg-purple-100 text-purple-700",
  READY: "bg-teal-100 text-teal-700",
  OUT_FOR_DELIVERY: "bg-orange-100 text-orange-700",
  DELIVERED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-700",
};

export default function OrdersPage() {
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [payingOrderId, setPayingOrderId] = useState(null);

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders = async () => {
    try {
      const response = await getMyOrders();
      setOrders(response.data.content || []);
    } catch (error) {
      console.error("Failed to load orders:", error);
      setMessage("Unable to load your orders. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handlePayNow = async (order) => {
    setMessage("");
    setPayingOrderId(order.id);

    try {
      const { data } = await resumeOrderPayment(order.id);

      if (!data.clientSecret) {
        throw new Error("Payment session could not be created.");
      }

      navigate("/payment", {
        state: {
          orderId: data.orderId,
          paymentId: data.paymentId,
          amount: data.amount,
          clientSecret: data.clientSecret,
        },
      });
    } catch (error) {
      console.error("Unable to resume payment:", error);

      setMessage(
        error.response?.data?.message ||
          error.message ||
          "Unable to resume payment. Please try again."
      );
    } finally {
      setPayingOrderId(null);
    }
  };

  const handleCancel = async (id) => {
    setMessage("");

    try {
      await cancelOrder(id);
      await loadOrders();

      setMessage(
        `Order #${id} cancelled. Refund status is shown by payment history.`
      );
    } catch (error) {
      setMessage(
        error.response?.data?.message ||
          "The order could not be cancelled."
      );
    }
  };

  if (loading) {
    return (
      <MainLayout>
        <div className="p-10">Loading...</div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <section className="min-h-screen bg-slate-50 py-16">
        <div className="mx-auto max-w-6xl px-6">
          <h1 className="mb-10 text-4xl font-bold">
            My Orders
          </h1>

          {message && (
            <p
              role="status"
              className="mb-5 rounded-lg bg-orange-50 p-3 text-orange-800"
            >
              {message}
            </p>
          )}

          {orders.length === 0 && (
            <div className="rounded-xl bg-white p-6 shadow">
              No orders found
            </div>
          )}

          <div className="space-y-6">
            {orders.map((order) => (
              <div
                key={order.id}
                className="rounded-xl bg-white p-6 shadow"
              >
                <div className="flex justify-between">
                  <div>
                    <h2 className="text-xl font-bold">
                      Order #{order.id}
                    </h2>

                    <p className="text-gray-500">
                      Order {order.orderNumber}
                    </p>
                  </div>

                  <div>
                    <span
                      className={`rounded-full px-4 py-2 text-sm font-medium ${
                        statusColor[order.status] ||
                        "bg-gray-100 text-gray-700"
                      }`}
                    >
                      {order.status}
                    </span>
                  </div>
                </div>

                <div className="mt-6 space-y-2">
                  {order.items.map((item) => (
                    <div
                      key={item.id}
                      className="flex justify-between"
                    >
                      <span>
                        {item.itemName}
                        {" x "}
                        {item.quantity}
                      </span>

                      <span>₹{item.subtotal}</span>
                    </div>
                  ))}
                </div>

                <div className="mt-6 border-t pt-4">
                  <h3 className="font-bold">
                    Total ₹{order.totalAmount}
                  </h3>
                </div>

                {order.status === "PENDING" && (
                  <button
                    onClick={() => handlePayNow(order)}
                    disabled={payingOrderId === order.id}
                    className="mt-4 mr-3 rounded-lg bg-orange-500 px-5 py-2 font-medium text-white hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {payingOrderId === order.id
                      ? "Loading payment..."
                      : `Pay Now ₹${order.totalAmount}`}
                  </button>
                )}

                {["PENDING", "CONFIRMED", "ACCEPTED"].includes(
                  order.status
                ) && (
                  <button
                    onClick={() => handleCancel(order.id)}
                    className="mt-4 rounded-lg border border-red-300 px-4 py-2 font-medium text-red-700 hover:bg-red-50"
                  >
                    Cancel order and request refund
                  </button>
                )}

                {order.status === "OUT_FOR_DELIVERY" && (
                  <DriverTrackingMap orderId={order.id} />
                )}
              </div>
            ))}
          </div>
        </div>
      </section>
    </MainLayout>
  );
}
