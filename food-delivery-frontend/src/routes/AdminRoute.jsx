import { Navigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";

export default function AdminRoute({ children }) {
  const user = useSelector((state) => state.auth.user);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== "ADMIN" && user.role !== "RESTAURANT_OWNER") {
    return <Navigate to="/" replace />;
  }

  return children;
}
