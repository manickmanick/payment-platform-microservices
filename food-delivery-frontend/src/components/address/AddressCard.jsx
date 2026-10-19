import Button from "../ui/Button";

export default function AddressCard({ address, onDelete, onEdit }) {
  return (
    <div className="rounded-xl bg-white p-6 shadow">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h3 className="text-xl font-bold">{address.label}</h3>

            {address.primaryAddress && (
              <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-700">
                Default
              </span>
            )}
          </div>

          <div className="mt-4 space-y-1 text-slate-700">
            <p>{address.line1}</p>

            {address.line2 && <p>{address.line2}</p>}

            <p>
              {address.city}, {address.state}
            </p>

            <p>{address.postalCode}, {address.country}</p>
          </div>
        </div>

        <div className="flex flex-wrap gap-3">

          <Button
            className="bg-slate-700 hover:bg-slate-800"
            onClick={() => onEdit(address)}
          >
            Edit
          </Button>

          <Button
            className="bg-red-500 hover:bg-red-600"
            onClick={() => onDelete(address.id)}
          >
            Delete
          </Button>
        </div>
      </div>
    </div>
  );
}
