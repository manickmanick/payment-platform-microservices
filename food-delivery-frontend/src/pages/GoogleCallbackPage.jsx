import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { loginSuccess } from "../redux/slices/authSlice";
import { getCurrentUser } from "../api/authApi";

export default function GoogleCallbackPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  useEffect(() => {
    const token = new URLSearchParams(window.location.hash.slice(1)).get("access_token");
    if (!token) { navigate("/login", { replace: true }); return; }
    localStorage.setItem("token", token);
    window.history.replaceState(null, "", window.location.pathname);
    getCurrentUser().then(({ data }) => {
      const user = { ...data, role: data.role?.replace(/^ROLE_/, "") || "USER" };
      localStorage.setItem("user", JSON.stringify(user));
      dispatch(loginSuccess(user));
      navigate("/", { replace: true });
    }).catch(() => navigate("/login", { replace: true }));
  }, [dispatch, navigate]);
  return <main className="p-10 text-center">Signing you in with Google…</main>;
}
