
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";
import AddressForm from "../components/address/AddressForm";

import {
  getAddressById,
  updateAddress,
} from "../api/addressApi";

export default function EditAddressPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [address, setAddress] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const loadAddress = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await getAddressById(id);

        if (!cancelled) {
          setAddress(response.data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
              err.response?.data?.message ||
              err.message ||
              "Unable to load address"
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadAddress();

    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleUpdate = async (form) => {
    try {
      setSaving(true);
      setError("");

      await updateAddress(id, {
        label: form.label,
        line1: form.line1,
        line2: form.line2 || "",
        city: form.city,
        state: form.state,
        postalCode: form.postalCode,
        country: form.country,
        phoneNumber: form.phoneNumber || "",
        smsOptIn: Boolean(form.smsOptIn),
        primaryAddress: Boolean(form.primaryAddress),
      });

      navigate("/addresses", {
        state: { message: "Address updated successfully" },
      });
    } catch (err) {
      setError(
          err.response?.data?.message ||
          "Unable to update address"
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
        <MainLayout>
          <div className="flex min-h-screen items-center justify-center">
            Loading address...
          </div>
        </MainLayout>
    );
  }

  if (!address) {
    return (
        <MainLayout>
          <section className="min-h-screen bg-slate-50 px-6 py-16">
            <div className="mx-auto max-w-4xl rounded-xl bg-white p-6 shadow">
              <p className="text-red-600">
                {error || "Address not found"}
              </p>

              <button
                  type="button"
                  onClick={() => navigate("/addresses")}
                  className="mt-4 rounded-lg bg-orange-600 px-4 py-2 text-white"
              >
                Back to Addresses
              </button>
            </div>
          </section>
        </MainLayout>
    );
  }

  return (
      <MainLayout>
        <section className="min-h-screen bg-slate-50 py-16">
          <div className="mx-auto max-w-4xl px-6">
            <h1 className="mb-10 text-4xl font-bold">
              Edit Address
            </h1>

            {error && (
                <div
                    role="alert"
                    className="mb-5 rounded-lg bg-red-50 p-3 text-red-700"
                >
                  {error}
                </div>
            )}

            <AddressForm
                key={address.id}
                initialValues={address}
                onSubmit={handleUpdate}
                submitButtonText={
                  saving ? "Updating..." : "Update Address"
                }
                disabled={saving}
            />
          </div>
        </section>
      </MainLayout>
  );
}
