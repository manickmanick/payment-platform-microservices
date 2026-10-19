import {
  useEffect,
  useState,
} from "react";

import MainLayout from "../layouts/MainLayout";

import Button from "../components/ui/Button";

import {
  getRestaurantOrders,
  acceptOrder,
  updateOrderStatus,
} from "../api/ownerOrderApi";

export default function OwnerOrdersPage() {

  const [
    orders,
    setOrders,
  ] = useState([]);

  const [
    loading,
    setLoading,
  ] = useState(true);

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders =
    async () => {

      try {

        const response =
          await getRestaurantOrders();

        setOrders(
          response.data.content
        );

      } catch (error) {

        console.error(error);

      } finally {

        setLoading(false);

      }
    };

  const handleAccept =
    async (orderId) => {

      await acceptOrder(
        orderId
      );

      loadOrders();
    };

  const handleStatusUpdate =
    async (
      orderId,
      status
    ) => {

      await updateOrderStatus(
        orderId,
        status
      );

      loadOrders();
    };

  if (loading) {
    return (
      <MainLayout>
        <div className="p-10">
          Loading...
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>

      <section className="min-h-screen bg-slate-50 py-16">

        <div className="mx-auto max-w-6xl px-6">

          <h1 className="mb-10 text-4xl font-bold">
            Restaurant Orders
          </h1>

          {orders.length === 0 && (
            <div className="rounded-xl bg-white p-6 shadow">
              No Orders Found
            </div>
          )}

          <div className="space-y-6">

            {orders.map(
              (order) => (
                <div
                  key={order.id}
                  className="rounded-xl bg-white p-6 shadow"
                >
                  <div className="flex justify-between">

                    <div>

                      <h2 className="text-xl font-bold">
                        Order #{order.id}
                      </h2>

                      <p>
                        Customer: {order.customerId}
                      </p>

                      <p>
                        Order: {order.orderNumber}
                      </p>

                    </div>

                    <div>
                      <span className="rounded-full bg-orange-100 px-4 py-2 text-sm font-medium text-orange-600">
                        {order.status}
                      </span>
                    </div>

                  </div>

                  <div className="mt-5">

                    <h3 className="mb-3 font-semibold">
                      Items
                    </h3>

                    {order.items.map(
                      (item) => (
                        <div
                          key={item.id}
                          className="flex justify-between py-1"
                        >
                          <span>
                            {
                              item.itemName
                            }
                            {" x "}
                            {
                              item.quantity
                            }
                          </span>

                          <span>
                            ₹
                            {
                              item.subtotal
                            }
                          </span>
                        </div>
                      )
                    )}

                  </div>

                  <div className="mt-5 border-t pt-4">

                    <h3 className="font-bold">
                      Total ₹
                      {
                        order.totalAmount
                      }
                    </h3>

                  </div>

                  <div className="mt-6 flex flex-wrap gap-3">

                    {order.status ===
                      "PLACED" && (
                      <Button
                        onClick={() =>
                          handleAccept(
                            order.id
                          )
                        }
                      >
                        Accept
                      </Button>
                    )}

                    {order.status ===
                      "ACCEPTED" && (
                      <Button
                        onClick={() =>
                          handleStatusUpdate(
                            order.id,
                            "PREPARING"
                          )
                        }
                      >
                        Preparing
                      </Button>
                    )}

                    {order.status ===
                      "PREPARING" && (
                      <Button
                        onClick={() =>
                          handleStatusUpdate(
                            order.id,
                            "READY"
                          )
                        }
                      >
                        Ready for Pickup
                      </Button>
                    )}

                  </div>

                </div>
              )
            )}

          </div>

        </div>

      </section>

    </MainLayout>
  );
}
