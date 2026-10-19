import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";

import AddressForm from "../components/address/AddressForm";

import { getAddressById, updateAddress } from "../api/addressApi";

export default function EditAddressPage() {
  const { id } = useParams();

  const navigate = useNavigate();

  const [address, setAddress] = useState(null);

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAddress();
  }, []);

  const loadAddress = async () => {
    try {
      const response = await getAddressById(id);

      setAddress(response.data);
    } catch (error) {
      console.error(error);

      alert("Unable to load address");

      navigate("/addresses");
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (form) => {
    try {
      await updateAddress(id, form);

      alert("Address updated successfully");

      navigate("/addresses");
    } catch (error) {
      console.error(error);

      alert(error.response?.data?.message || "Unable to update address");
    }
  };

  if (loading) {
    return (
      <MainLayout>
        <div className="flex min-h-screen items-center justify-center">
          Loading...
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <section className="min-h-screen bg-slate-50 py-16">
        <div className="mx-auto max-w-4xl px-6">
          <h1 className="mb-10 text-4xl font-bold">Edit Address</h1>

          <AddressForm
            initialValues={address}
            onSubmit={handleUpdate}
            submitButtonText="Update Address"
          />
        </div>
      </section>
    </MainLayout>
  );
}
