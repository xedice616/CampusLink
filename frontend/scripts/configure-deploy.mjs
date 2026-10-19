import {readFile,writeFile} from 'node:fs/promises';
const input=process.argv[2];
if(!input){console.error('Usage: node scripts/configure-deploy.mjs https://YOUR-BACKEND.onrender.com');process.exit(1);}
let url;
try{url=new URL(input);}catch{console.error('Provide a valid HTTPS backend origin.');process.exit(1);}
if(url.protocol!=='https:'||url.username||url.password||url.search||url.hash||url.pathname!=='/'||url.hostname.toLowerCase().includes('replace-with')){
 console.error('Use the real HTTPS origin only, without credentials, path, query or fragment.');process.exit(1);
}
const configFile=new URL('../vercel.json',import.meta.url);
const config=JSON.parse(await readFile(configFile,'utf8'));
const rewrite=config.rewrites.find(rule=>rule.source==='/api/:path*');
if(!rewrite)throw new Error('Missing API rewrite rule');
rewrite.destination=url.origin+'/api/:path*';
await writeFile(configFile,JSON.stringify(config,null,2)+'\n');
console.log('Vercel API rewrite configured for '+url.origin+'. Commit and push vercel.json before deploying.');
