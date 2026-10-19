export default function Badge({
  children,
}) {
  return (
    <span
      className="
        rounded-full
        bg-orange-100
        px-3
        py-1
        text-sm
        font-medium
        text-orange-600
      "
    >
      {children}
    </span>
  );
}