import AppRoutes from "./routes/AppRoutes";
import { useSelector } from "react-redux";
// import socket from "./socket";
import { Toaster, toast } from "react-hot-toast";
import { useEffect } from "react";

function App() {
  const user = useSelector((state) => state.auth.user);
  console.log("Current User:", user);
  useEffect(() => {
    console.log("Current User:", user);

    if (user) {
      console.log("Joining Room:", user.id);

      // socket.emit("join-user", user.id);

      console.log("join-user emitted");
    }
  }, [user]);

  useEffect(() => {
    // socket.on("order-status-updated", (data) => {
    //   toast.success(`Order #${data.orderId} is now ${data.status}`);
    // });

    return () => {
      // socket.off("order-status-updated");
    };
  }, []);
  return (
    <>
      <AppRoutes />
      <Toaster position="top-right" />
    </>
  );
}

export default App;
