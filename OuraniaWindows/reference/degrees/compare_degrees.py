import os,re,json,sys,statistics as st
HERE=os.path.dirname(os.path.abspath(__file__))
t=open(os.path.join(HERE,'360_degrees_interpretations.pasted.txt'),encoding='utf8').read()
E={e[0]:e for e in re.findall(r'"([a-z]+_\d+)":\s*\{\s*"title":\s*"([^"]*)",\s*"summary":\s*"((?:[^"\\]|\\.)*)",\s*"fullText":\s*"((?:[^"\\]|\\.)*)"',t,re.S)}
D=json.load(open(os.path.join(HERE,'..','..','src','main','resources','data','degree_interpretations.json'),encoding='utf-8-sig'))
def words(s): return re.findall(r"[a-z']+",re.sub(r'\[[\d,\s-]+\]','',s.lower()))
def grams(s,n):
    w=words(s); return {' '.join(w[i:i+n]) for i in range(len(w)-n+1)}
STOP=set('the a an and or of to in is are with by for that this who their its it as on at from be can may into than but not they them his her he she which while through one own nature degree'.split())
def content(s): return {w for w in words(s) if w not in STOP and len(w)>3}
named=lambda k: bool(re.search(r'\b(Sepharial|Janduz|Kozminsky|Henson|Matthews|Weber|Charubel|Muir)\b',E[k][3]))
norm=lambda s: ' '.join(words(s))
rows=[]
for k,v in D.items():
    a=v.get('summary','')+' '+v.get('fullText','')
    if k not in E: rows.append((k,None,None,None,None)); continue
    b=E[k][2]+' '+E[k][3]
    g3a=grams(a,3); g3=len(g3a&grams(b,3))/max(1,len(g3a))
    ca=content(a); cw=len(ca&content(b))/max(1,len(ca))
    rows.append((k,g3,cw,norm(v.get('fullText',''))==norm(E[k][3]),named(k)))
print('entries: app',len(D),'stray',len(E))
print('app degrees with no entry in the stray file:',[r[0] for r in rows if r[1] is None])
for lab,sel in [('stray entry names authors',True),('stray entry names nobody',False)]:
    r=[x for x in rows if x[1] is not None and x[4]==sel]
    g=[x[1] for x in r]; c=[x[2] for x in r]
    print(f"{lab}: n={len(r)} | word-for-word identical (citation marks aside): {sum(1 for x in r if x[3])} | share of the app entry's 3-word runs also in the stray entry: median {st.median(g):.2f}, min {min(g):.2f}, max {max(g):.2f}, >=0.50 in {sum(1 for x in g if x>=.5)}, <0.05 in {sum(1 for x in g if x<.05)} | content words shared: median {st.median(c):.2f}")
if len(sys.argv)>1:
    for k in sys.argv[1:]:
        print('\nAPP  ',k,':',D[k]['fullText'][:280]); print('STRAY',k,':',E[k][3][:280])
