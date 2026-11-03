/** Confidence in words first, number second. Farmers read "Very likely", not "0.87". */
export function ConfidenceMeter({ label, value }: { label: string; value: number }) {
  const pct = Math.round(value * 100);
  return (
    <div className="meter">
      <div className="meter-row">
        <span className="meter-label">{label}</span>
        <span className="meter-pct">{pct}%</span>
      </div>
      <div className="meter-track" role="meter" aria-valuemin={0} aria-valuemax={100} aria-valuenow={pct} aria-label={label}>
        <span className="meter-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
