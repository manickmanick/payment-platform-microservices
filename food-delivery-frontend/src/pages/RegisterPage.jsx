import { useState } from "react";
import { Link } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import { registerUser } from "../api/authApi";

export default function RegisterPage() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    isRestaurantOwner: false,
    secretCode: "",
  });

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [registered, setRegistered] = useState(false);

  // Validate the form before sending the registration request
  const validateForm = () => {
    const localErrors = {};

    if (!form.name.trim()) {
      localErrors.name = "Full name is required";
    }

    if (!form.email.trim()) {
      localErrors.email = "Email address is required";
    } else if (!/\S+@\S+\.\S+/.test(form.email)) {
      localErrors.email = "Please enter a valid email address";
    }

    if (!form.password.trim()) {
      localErrors.password = "Password is required";
    } else if (form.password.length < 8) {
      localErrors.password = "Password must be at least 8 characters long";
    }

    if (!form.confirmPassword.trim()) {
      localErrors.confirmPassword = "Please confirm your password";
    } else if (form.password !== form.confirmPassword) {
      localErrors.confirmPassword = "Passwords do not match";
    }

    // A restaurant owner must provide a secret code
    if (form.isRestaurantOwner && !form.secretCode.trim()) {
      localErrors.secretCode = "Secret code is required";
    }

    setErrors(localErrors);

    return Object.keys(localErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrors({});

    if (!validateForm()) return;

    try {
      setLoading(true);

      await registerUser({
        name: form.name,
        email: form.email,
        password: form.password,
        confirmPassword:form.confirmPassword,
        isRestaurantOwner: form.isRestaurantOwner,
        secretCode: form.isRestaurantOwner ? form.secretCode : undefined,
      });

      console.count("Registration submitted");

      setRegistered(true);
    } catch (error) {
      console.error(error);

      setErrors({
        api: error.response?.data?.message || "Something went wrong",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field, value) => {
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));

    setErrors((prev) => ({
      ...prev,
      [field]: "",
      api: "",
    }));
  };

  if (registered) {
    return (
      <MainLayout>
        <section className="min-h-[70vh] flex items-center justify-center px-6">
          <div className="max-w-lg rounded-2xl bg-white p-8 text-center shadow">
            <h1 className="text-3xl font-bold">Verify your email</h1>

             <p className="mt-3 text-slate-600">
              We sent an account verification link to {form.email}. Verify the
              link before signing in.
            </p>

            <Link
              to="/login"
              className="mt-6 inline-block font-semibold text-orange-600"
            >
              Continue to sign in
            </Link>
          </div>
        </section>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <section className="min-h-[85vh] bg-slate-50 py-20 flex items-center justify-center">
        <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-md border border-gray-100">
          <h1 className="mb-2 text-center text-3xl font-bold text-gray-800">
            Create Account
          </h1>

          <p className="mb-8 text-center text-slate-500">
            Register to start ordering food
          </p>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Full Name */}
            <div>
              <Input
                placeholder="Full Name"
                value={form.name}
                onChange={(e) => handleInputChange("name", e.target.value)}
                hasError={!!errors.name}
              />

              {errors.name && (
                <p className="mt-1 text-xs font-medium text-red-500 pl-1">
                  {errors.name}
                </p>
              )}
            </div>

            {/* Email */}
            <div>
              <Input
                type="email"
                placeholder="Email Address"
                value={form.email}
                onChange={(e) => handleInputChange("email", e.target.value)}
                hasError={!!errors.email}
              />

              {errors.email && (
                <p className="mt-1 text-xs font-medium text-red-500 pl-1">
                  {errors.email}
                </p>
              )}
            </div>

            {/* Password */}
            <div>
              <Input
                type="password"
                placeholder="Password"
                value={form.password}
                onChange={(e) => handleInputChange("password", e.target.value)}
                hasError={!!errors.password}
              />

              {errors.password && (
                <p className="mt-1 text-xs font-medium text-red-500 pl-1">
                  {errors.password}
                </p>
              )}
            </div>

            {/* Confirm Password */}
            <div>
              <Input
                type="password"
                placeholder="Confirm Password"
                value={form.confirmPassword}
                onChange={(e) =>
                  handleInputChange("confirmPassword", e.target.value)
                }
                hasError={!!errors.confirmPassword}
              />

              {errors.confirmPassword && (
                <p className="mt-1 text-xs font-medium text-red-500 pl-1">
                  {errors.confirmPassword}
                </p>
              )}
            </div>

            {/* Restaurant Owner Checkbox */}
            <div className="rounded-xl border border-gray-200 p-4">
              <label className="flex cursor-pointer items-center gap-3">
                <input
                  type="checkbox"
                  checked={form.isRestaurantOwner}
                  onChange={(e) => {
                    const checked = e.target.checked;

                    setForm((prev) => ({
                      ...prev,
                      isRestaurantOwner: checked,
                      secretCode: checked ? prev.secretCode : "",
                    }));

                    setErrors((prev) => ({
                      ...prev,
                      secretCode: "",
                      api: "",
                    }));
                  }}
                  className="h-4 w-4 accent-orange-500"
                />

                <span className="text-sm font-medium text-gray-700">
                  I am a Restaurant Owner
                </span>
              </label>

              <p className="mt-2 ml-7 text-xs text-slate-500">
                Select this option if you want to register and manage a
                restaurant.
              </p>
            </div>

            {/* Secret Code: visible only for restaurant owners */}
            {form.isRestaurantOwner && (
              <div>
                <Input
                  type="password"
                  placeholder="Restaurant Owner Secret Code"
                  value={form.secretCode}
                  onChange={(e) =>
                    handleInputChange("secretCode", e.target.value)
                  }
                  hasError={!!errors.secretCode}
                />

                {errors.secretCode && (
                  <p className="mt-1 text-xs font-medium text-red-500 pl-1">
                    {errors.secretCode}
                  </p>
                )}
              </div>
            )}

            {/* Backend Error */}
            {errors.api && (
              <div className="rounded-xl border border-red-200 bg-red-50/50 px-4 py-3 text-sm text-red-600 font-medium">
                {errors.api}
              </div>
            )}

            <Button
              type="submit"
              className="w-full py-3.5 mt-2"
              disabled={loading}
            >
              {loading
                ? "Creating Account..."
                : form.isRestaurantOwner
                  ? "Register as Restaurant Owner"
                  : "Register"}
            </Button>

            <p className="text-center text-sm text-slate-500 pt-2">
              Already have an account?{" "}
              <Link
                to="/login"
                className="font-semibold text-orange-500 hover:text-orange-600 transition"
              >
                Sign In
              </Link>
            </p>
          </form>
        </div>
      </section>
    </MainLayout>
  );
}
