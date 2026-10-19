import { Navigate } from "react-router-dom";
import { useSelector } from "react-redux";

export default function PublicRoute({ children }) {
  const user = useSelector((state) => state.auth.user);
  console.log("user -> ", user);

  if (user) {
    return <Navigate to="/" replace />;
  }

  return children;
}
