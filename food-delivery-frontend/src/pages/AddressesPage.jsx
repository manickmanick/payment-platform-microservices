import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";

import AddressForm from "../components/address/AddressForm";
import AddressCard from "../components/address/AddressCard";

import {
  getAddresses,
  createAddress,
  deleteAddress,
} from "../api/addressApi";

export default function AddressPage() {
  const navigate = useNavigate();

  const [addresses, setAddresses] = useState([]);

  useEffect(() => {
    loadAddresses();
  }, []);

  const loadAddresses = async () => {
    try {
      const response = await getAddresses();

      setAddresses(response.data);
    } catch (error) {
      console.error(error);
    }
  };

  const handleCreate = async (form) => {
    try {
      await createAddress(form);

      await loadAddresses();
    } catch (error) {
      console.error(error);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this address?")) {
      return;
    }

    try {
      await deleteAddress(id);

      await loadAddresses();
    } catch (error) {
      console.error(error);
    }
  };

  const handleEdit = (address) => {
    navigate(`/addresses/${address.id}/edit`);
  };

  return (
    <MainLayout>
      <section className="min-h-screen bg-slate-50 py-16">
        <div className="mx-auto max-w-6xl px-6">
          <h1 className="mb-10 text-4xl font-bold">My Addresses</h1>

          <AddressForm
            onSubmit={handleCreate}
            submitButtonText="Save Address"
          />

          <div className="space-y-5">
            {addresses.map((address) => (
              <AddressCard
                key={address.id}
                address={address}
                onDelete={handleDelete}
                onEdit={handleEdit}
              />
            ))}
          </div>
        </div>
      </section>
    </MainLayout>
  );
}
