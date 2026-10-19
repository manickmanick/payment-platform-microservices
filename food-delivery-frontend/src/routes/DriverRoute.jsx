import { Navigate } from "react-router-dom";
import { useSelector } from "react-redux";

export default function DriverRoute({ children }) {
  const user = useSelector((state) => state.auth.user);
  if (!user) return <Navigate to="/login" replace />;
  return user.role === "DRIVER" ? children : <Navigate to="/" replace />;
}
