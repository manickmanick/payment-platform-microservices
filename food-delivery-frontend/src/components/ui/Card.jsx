export default function Card({
  children,
  className = "",
}) {
  return (
    <div
      className={`
        rounded-2xl
        bg-white
        shadow-md
        transition
        hover:shadow-xl
        ${className}
      `}
    >
      {children}
    </div>
  );
}