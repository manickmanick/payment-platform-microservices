export default function Input({
  type = "text",
  placeholder,
  value,
  onChange,
  hasError = false,
  className = "",
}) {
  return (
    <input
      type={type}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      className={`
        w-full
        rounded-xl
        border
        px-4
        py-3
        outline-none
        transition
        duration-150
        
        /* Error and normal border toggle states */
        ${
          hasError
            ? "border-red-400 bg-red-50/20 focus:border-red-500"
            : "border-gray-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/10"
        }
        
        ${className}
      `}
    />
  );
}
