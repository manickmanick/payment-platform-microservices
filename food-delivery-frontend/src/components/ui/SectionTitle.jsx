export default function SectionTitle({
  title,
  subtitle,
}) {
  return (
    <div className="mb-12 text-center">
      <h2 className="text-4xl font-bold">
        {title}
      </h2>

      {subtitle && (
        <p className="mt-3 text-gray-500">
          {subtitle}
        </p>
      )}
    </div>
  );
}