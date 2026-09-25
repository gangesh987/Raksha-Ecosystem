
import { motion } from "framer-motion";
import { advancedFeatures } from "./advancedFeatures";

export function AdvancedFeaturePanel() {
  return (
    <section className="panel" style={{ marginTop: 24 }}>
      <div className="section-kicker">SENTINEL / ADVANCED INTELLIGENCE</div>
      <h2>Beyond a single risk score.</h2>
      <p className="muted">RakshaCall now models trajectory, tactics, evidence integrity, signal quality and intervention policy around the core conversation detector.</p>
      <div className="feature-grid">
        {advancedFeatures.map((f, i) => (
          <motion.div key={f.id} className="feature-card"
            initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * .04 }}>
            <strong>{f.title}</strong>
            <span>{f.detail}</span>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
