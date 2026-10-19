import { Link } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";

import Button from "../components/ui/Button";

export default function OrderSuccessPage() {
  return (
    <MainLayout>
      <div className="mx-auto max-w-xl py-20 text-center">
        <h1 className="mb-4 text-5xl">🎉</h1>

        <h2 className="mb-6 text-3xl font-bold">Order Placed Successfully</h2>

        <Link to="/orders">
          <Button>View Orders</Button>
        </Link>
      </div>
    </MainLayout>
  );
}
