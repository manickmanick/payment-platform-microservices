export default function Button({
  children,
  onClick,
  type = "button",
  className = "",
  disabled = false,
}) {
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={`
        rounded-xl
        bg-orange-500
        px-5
        py-3
        font-semibold
        text-white
        transition
        enabled:hover:bg-orange-600
        disabled:bg-gray-400
        disabled:text-gray-200
        disabled:cursor-not-allowed
        
        ${className}
      `}
    >
      {children}
    </button>
  );
}