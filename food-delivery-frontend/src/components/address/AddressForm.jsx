import { useEffect, useState } from "react";

import Input from "../ui/Input";
import Button from "../ui/Button";

const INITIAL_FORM = {
  label: "",
  line1: "",
  line2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "India",
  phoneNumber: "",
  smsOptIn: false,
  primaryAddress: false,
};

export default function AddressForm({
  initialValues = INITIAL_FORM,
  onSubmit,
  submitButtonText = "Save Address",
}) {
  const [form, setForm] = useState(INITIAL_FORM);

  const [errors, setErrors] = useState({});

  useEffect(() => {
    setForm({
      ...INITIAL_FORM,
      ...initialValues,
    });
  }, [initialValues]);

  const validateForm = () => {
    const newErrors = {};

    if (!form.label.trim()) {
      newErrors.label = "Label is required";
    }

    if (!form.line1.trim()) {
      newErrors.line1 = "Address Line 1 is required";
    }

    if (!form.city.trim()) {
      newErrors.city = "City is required";
    }

    if (!form.state.trim()) {
      newErrors.state = "State is required";
    }

    if (!form.postalCode.trim()) {
      newErrors.postalCode = "Pincode is required";
    }

    setErrors(newErrors);

    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (field, value) => {
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));

    if (errors[field]) {
      setErrors((prev) => ({
        ...prev,
        [field]: "",
      }));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    onSubmit(form);
  };

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      className="mb-10 rounded-xl bg-white p-6 shadow"
    >
      <div className="grid gap-4 md:grid-cols-2">
        <div>
          <Input
            placeholder="Label (Home, Office...)"
            value={form.label}
            onChange={(e) => handleChange("label", e.target.value)}
            hasError={!!errors.label}
          />

          {errors.label && (
            <p className="mt-1 pl-1 text-xs font-medium text-red-500">
              {errors.label}
            </p>
          )}
        </div>

        <div>
          <Input
            placeholder="Address Line 1"
            value={form.line1}
            onChange={(e) => handleChange("line1", e.target.value)}
            hasError={!!errors.line1}
          />

          {errors.line1 && (
            <p className="mt-1 pl-1 text-xs font-medium text-red-500">
              {errors.line1}
            </p>
          )}
        </div>

        <div>
          <Input
            placeholder="Address Line 2 (Optional)"
            value={form.line2}
            onChange={(e) => handleChange("line2", e.target.value)}
          />
        </div>

        <div>
          <Input
            placeholder="City"
            value={form.city}
            onChange={(e) => handleChange("city", e.target.value)}
            hasError={!!errors.city}
          />

          {errors.city && (
            <p className="mt-1 pl-1 text-xs font-medium text-red-500">
              {errors.city}
            </p>
          )}
        </div>

        <div>
          <Input
            placeholder="State"
            value={form.state}
            onChange={(e) => handleChange("state", e.target.value)}
            hasError={!!errors.state}
          />

          {errors.state && (
            <p className="mt-1 pl-1 text-xs font-medium text-red-500">
              {errors.state}
            </p>
          )}
        </div>

        <div>
          <Input
            placeholder="Pincode"
            maxLength={6}
            value={form.postalCode}
            onChange={(e) => handleChange("postalCode", e.target.value)}
            hasError={!!errors.postalCode}
          />

          {errors.postalCode && (
            <p className="mt-1 pl-1 text-xs font-medium text-red-500">
              {errors.postalCode}
            </p>
          )}
        </div>

        <div className="md:col-span-2">
          <Input placeholder="Country" value={form.country} onChange={(e) => handleChange("country", e.target.value)} />
        </div>

        <div className="md:col-span-2"><Input type="tel" placeholder="Phone number (for delivery updates)" value={form.phoneNumber} onChange={(e)=>handleChange("phoneNumber",e.target.value)} /></div>
        <div className="md:col-span-2 flex items-center gap-3"><input id="sms-updates" type="checkbox" checked={form.smsOptIn} onChange={(e)=>handleChange("smsOptIn",e.target.checked)} /><label htmlFor="sms-updates" className="cursor-pointer text-sm">Send me delivery status updates by SMS</label></div>

        <div className="md:col-span-2 flex items-center gap-3">
          <input
            id="default-address"
            type="checkbox"
            checked={form.primaryAddress}
            onChange={(e) => handleChange("primaryAddress", e.target.checked)}
          />

          <label htmlFor="default-address" className="cursor-pointer text-sm">
            Make this my default address
          </label>
        </div>
      </div>

      <Button type="submit" className="mt-6">
        {submitButtonText}
      </Button>
    </form>
  );
}
