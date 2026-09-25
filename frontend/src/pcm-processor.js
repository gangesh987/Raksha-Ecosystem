class RakshaPCMProcessor extends AudioWorkletProcessor {
  constructor(){ super(); this.buffer=[]; this.target=16000; }
  process(inputs){
    const input=inputs[0]&&inputs[0][0];
    if(!input) return true;
    const ratio=sampleRate/this.target;
    for(let i=0;i<input.length;i+=ratio){
      const idx=Math.floor(i);
      const s=Math.max(-1,Math.min(1,input[idx]||0));
      this.buffer.push(s);
    }
    if(this.buffer.length>=1600){
      const chunk=this.buffer.splice(0,1600);
      const out=new Int16Array(chunk.length);
      for(let i=0;i<chunk.length;i++) out[i]=chunk[i]<0?chunk[i]*0x8000:chunk[i]*0x7fff;
      this.port.postMessage(out.buffer,[out.buffer]);
    }
    return true;
  }
}
registerProcessor('raksha-pcm-processor',RakshaPCMProcessor);
