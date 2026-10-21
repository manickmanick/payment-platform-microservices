import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useDispatch } from "react-redux";

import MainLayout from "../layouts/MainLayout";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import { loginUser } from "../api/authApi";
import { loginSuccess } from "../redux/slices/authSlice";

export default function LoginPage() {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // 1. Unified error states (both field-level and API level)
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 2. Client-side Form Validation
  const validateForm = () => {
    const localErrors = {};

    if (!email.trim()) {
      localErrors.email = "Email address is required";
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      localErrors.email = "Please enter a valid email address";
    }

    if (!password.trim()) {
      localErrors.password = "Password is required";
    }

    setErrors(localErrors);
    return Object.keys(localErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrors({});

    if (!validateForm()) return;

    try {
      setIsSubmitting(true);
      const response = await loginUser({ email, password });
      // const { accessToken, userId, name, email, role = "ROLE_USER" } = response.data;
      // const user = response.data.user || { id: userId, name, email, role: role.replace(/^ROLE_/, "") };

      const {
        accessToken,
        userId,
        name,
        email: responseEmail,
        role = "ROLE_USER",
      } = response.data;

      const user = response.data.user || {
        id: userId,
        name,
        email: responseEmail,
        role: role.replace(/^ROLE_/, ""),
      };
      localStorage.setItem("token", accessToken);
      localStorage.setItem("user", JSON.stringify(user));
      dispatch(loginSuccess(user));

      navigate("/");
    } catch (error) {
      console.error(error);
      // Handle API message or generic fallback
      setErrors({
        api: error.response?.data?.message || "Something went wrong",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Helper to clear errors instantly when typing
  const handleInputChange = (field, value, setter) => {
    setter(value);
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: "" }));
    }
    if (errors.api) {
      setErrors((prev) => ({ ...prev, api: "" }));
    }
  };

  return (
    <MainLayout>
      <section className="min-h-[80vh] bg-slate-50 py-20 flex items-center justify-center">
        <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-md border border-gray-100">
          <h1 className="mb-6 text-center text-3xl font-bold text-gray-800">
            Login
          </h1>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <a href={`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1"}/oauth2/authorization/google`} className="block w-full rounded-lg border border-gray-300 px-4 py-3 text-center font-semibold text-gray-700 hover:bg-gray-50">Continue with Google</a>
            {/* Email Field Block */}
            <div>
              <Input
                placeholder="Email Address"
                value={email}
                onChange={(e) =>
                  handleInputChange("email", e.target.value, setEmail)
                }
                hasError={!!errors.email}
              />
              {errors.email && (
                <p className="mt-1 text-xs font-medium text-red-500 pl-1">
                  ⚠️ {errors.email}
                </p>
              )}
            </div>

            {/* Password Field Block */}
            <div>
              <Input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) =>
                  handleInputChange("password", e.target.value, setPassword)
                }
                hasError={!!errors.password}
              />
              {errors.password && (
                <p className="mt-1 text-xs font-medium text-red-500 pl-1">
                  ⚠️ {errors.password}
                </p>
              )}
            </div>

            {/* API Error Callout */}
            {errors.api && (
              <div className="rounded-xl border border-red-200 bg-red-50/50 px-4 py-3 text-sm text-red-600 font-medium">
                ❌ {errors.api}
              </div>
            )}

            <Button
              type="submit"
              className="w-full py-3.5 mt-2"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Logging in..." : "Login"}
            </Button>

            <p className="text-center text-sm text-slate-500 pt-2">
              <Link to="/forgot-password" className="font-semibold text-orange-500">Forgot password?</Link>
            </p>
            <p className="text-center text-sm text-slate-500 pt-2">
              Don't have an account?{" "}
              <Link
                to="/register"
                className="font-semibold text-orange-500 hover:text-orange-600 transition"
              >
                Sign Up
              </Link>
            </p>
          </form>
        </div>
      </section>
    </MainLayout>
  );
}
