import { motion } from "framer-motion";

const features = [
  ["01","SCAM STAGE MACHINE","CONTACT → AUTHORITY → FEAR → ISOLATION → DEMAND → PAYMENT → ESCALATION"],
  ["02","MANIPULATION VELOCITY","Detects unusually rapid accumulation of coercive tactics."],
  ["03","INDEPENDENT VERIFICATION COACH","Guides the user to verify through a separately obtained official channel."],
  ["04","SAFETY BRAKE","One-tap end-call, trusted-contact, evidence-lock and safety checklist actions."],
  ["05","MODEL DISAGREEMENT GUARD","Reduces confidence when independent signals conflict."],
  ["06","PRIVACY BUDGET","Controls raw-media retention separately from derived risk events."],
  ["07","EVIDENCE INTEGRITY GRAPH","Links incident events into a tamper-evident provenance chain."],
  ["08","ATTACK REPLAY LAB","Replays synthetic scam scenarios without using private calls."],
];

export function NovelSafetyFeatures(){
  return <section className="panel" style={{marginTop:24}}>
    <div className="section-kicker">SENTINEL V4 / NOVELTY LAYER</div>
    <h2>Protection that understands how a scam evolves.</h2>
    <div className="feature-grid">
      {features.map(([n,t,d],i)=>
        <motion.div className="feature-card" key={n}
          initial={{opacity:0,y:14}} animate={{opacity:1,y:0}}
          transition={{delay:i*.035}}>
          <small>{n}</small><strong>{t}</strong><span>{d}</span>
        </motion.div>
      )}
    </div>
  </section>
}
