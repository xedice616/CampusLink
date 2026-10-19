// Existing numeric IDs remain compatible with saved/backend chat messages.
const legacy = [1,2,4,6,7,9].map(n=>({id:String(n),file:'sticky-'+n+'.jpg',label:'Community sticker '+n}));
const memes = [
  {id:'meme-1',file:'sticker-1.jpg',label:'Study dog: konular yetişmiyo'},
  {id:'meme-2',file:'sticker-2.jpg',label:'Waiting for Friday'},
  {id:'meme-3',file:'sticker-3.jpg',label:'Çok çok çok ders çalışmamız lazım'},
  {id:'meme-4',file:'sticker-4.jpg',label:'Şimdi çalış, sonra ağlarsın'},
  {id:'meme-6',file:'sticker-6.jpg',label:'Hayat hilesi: umursamayın'}
];
export const stickers = [...legacy,...memes];
export function stickerImage(id,className='chat-sticker') {
  const sticker=stickers.find(s=>s.id===id);
  return sticker ? '<img class="'+className+'" src="'+import.meta.env.BASE_URL+'assets/'+sticker.file+'" alt="'+sticker.label+'" loading="lazy" decoding="async">' : '';
}
export function messageSticker(text) {
  const match=typeof text==='string' && /^\[sticker:([a-z0-9-]+)\]$/.exec(text);
  return match ? stickerImage(match[1]) : '';
}
