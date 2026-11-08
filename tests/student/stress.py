"""Own tests: random cross-checks against brute force + max-size timing."""
import json, random, subprocess, pathlib, time, sys
from functools import lru_cache

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from public_validate import check

CMD = ['java', '-Xmx384m', '-cp', str(ROOT / 'Java' / 'build'), 'Main']

def run(reqs, timeout=60):
    data = ''.join(json.dumps(r) + '\n' for r in reqs)
    t = time.time()
    p = subprocess.run(CMD, input=data, capture_output=True, text=True, timeout=timeout)
    dt = time.time() - t
    if p.returncode:
        print(p.stderr[:2000]); sys.exit(1)
    return [json.loads(l) for l in p.stdout.splitlines()], dt

# brute forces
def lcs_len(a, b):
    prev = [0] * (len(b) + 1)
    for x in a:
        cur = [0]
        for j, y in enumerate(b):
            cur.append(prev[j] + 1 if x == y else max(prev[j + 1], cur[j]))
        prev = cur
    return prev[-1]

def merge_brute(sizes):
    @lru_cache(None)
    def f(bl):
        if len(bl) <= 1: return 0
        return min(bl[i] + bl[i+1] + f(bl[:i] + (bl[i] + bl[i+1],) + bl[i+2:]) for i in range(len(bl) - 1))
    return f(tuple(sizes))

def tree_brute(n, w, edges):
    best = 0
    for mask in range(1 << n):
        if any((mask >> u) & 1 and (mask >> v) & 1 for u, v in edges): continue
        best = max(best, sum(w[i] for i in range(n) if (mask >> i) & 1))
    return best

def occ(t, p):
    return [i for i in range(len(t) + 1) if t.startswith(p, i)]

def suffix_brute(s):
    n = len(s)
    sa = sorted(range(n), key=lambda i: s[i:])
    def cp(a, b):
        k = 0
        while a + k < n and b + k < n and s[a + k] == s[b + k]: k += 1
        return k
    lcp = ([0] + [cp(sa[i - 1], sa[i]) for i in range(1, n)]) if n else []
    best = max((cp(a, b) for a in range(n) for b in range(a + 1, n)), default=0)
    return sa, lcp, 'x' * best

def rs(alpha, lo, hi):
    return ''.join(random.choice(alpha) for _ in range(random.randint(lo, hi)))

# random cross-checks
def random_cases(seed=1, per_task=300):
    random.seed(seed)
    cases = []
    for _ in range(per_task):
        a, b = rs('abc', 0, 12), rs('abc ', 0, 12)
        cases.append(({'task': 'lcs', 'a': a, 'b': b}, {'length': lcs_len(a, b)}))
        sizes = [random.randint(0, 20) for _ in range(random.randint(0, 7))]
        cases.append(({'task': 'merge', 'sizes': sizes}, {'cost': merge_brute(sizes)}))
        n = random.randint(0, 10)
        edges = [[random.randrange(i), i] for i in range(1, n)]
        w = [random.randint(-5, 10) for _ in range(n)]
        cases.append(({'task': 'tree', 'n': n, 'weights': w, 'edges': edges}, {'weight': tree_brute(n, w, edges)}))
        t, p = rs('ab', 0, 20), rs('ab', 0, 4)
        for name in ('naive', 'kmp', 'rk'):
            cases.append(({'task': name, 'text': t, 'pattern': p}, {'positions': occ(t, p)}))
        s = rs('abc', 0, 25)
        sa, lcp, rep = suffix_brute(s)
        cases.append(({'task': 'suffix', 'text': s}, {'sa': sa, 'lcp': lcp, 'repeated': rep}))
        pats = [rs('ab', 1, 4) for _ in range(random.randint(0, 5))]
        texts = [rs('ab', 0, 15) for _ in range(random.randint(0, 4))]
        counts = [[sum(1 for i in range(len(tx)) if tx.startswith(pt, i)) for pt in pats] for tx in texts]
        cases.append(({'task': 'aho', 'patterns': pats, 'texts': texts}, {'counts': counts}))
    random.shuffle(cases)
    return cases

def run_random():
    cases = random_cases()
    answers, dt = run([c[0] for c in cases])
    bad = 0
    for (req, exp), ans in zip(cases, answers):
        if not check(req, ans, exp):
            bad += 1
            if bad <= 5: print('FAIL', json.dumps(req)[:300], '->', json.dumps(ans)[:300])
    print(f'random: {len(cases)} cases, {bad} failures, {dt:.2f}s')

# max-size cases
def run_big():
    n = 100000
    big = []
    big.append(('tree chain 100000', {'task': 'tree', 'n': n, 'weights': [1] * n,
                'edges': [[i, i + 1] for i in range(n - 1)]}, {'weight': 50000}))
    big.append(('kmp a*200000 / a*100000', {'task': 'kmp', 'text': 'a' * 200000, 'pattern': 'a' * 100000},
                {'positions': list(range(100001))}))
    big.append(('naive a*3000 / a*1500', {'task': 'naive', 'text': 'a' * 3000, 'pattern': 'a' * 1500},
                {'positions': list(range(1501))}))
    big.append(('rk a*10000 / a*2000 (all candidates)', {'task': 'rk', 'text': 'a' * 10000, 'pattern': 'a' * 2000},
                {'positions': list(range(8001))}))
    big.append(('suffix a*50000', {'task': 'suffix', 'text': 'a' * 50000},
                {'sa': list(range(49999, -1, -1)), 'lcp': list(range(50000)), 'repeated': 'x' * 49999}))
    random.seed(7)
    r = ''.join(random.choice('ab') for _ in range(1000)), ''.join(random.choice('ab') for _ in range(1000))
    big.append(('lcs 1000x1000', {'task': 'lcs', 'a': r[0], 'b': r[1]}, {'length': lcs_len(*r)}))
    sizes = [random.randint(0, 10**9) for _ in range(200)]
    big.append(('merge 200', {'task': 'merge', 'sizes': sizes}, None))
    big.append(('aho 10000 patterns, 100 dense texts',
                {'task': 'aho', 'patterns': ['a' * 10] * 10000, 'texts': ['a' * 2000] * 100},
                {'counts': [[1991] * 10000] * 100}))
    for name, req, exp in big:
        ans, dt = run([req])
        ok = 'n/a' if exp is None else check(req, ans[0], exp)
        print(f'{name}: {dt:.2f}s ok={ok}')

if __name__ == '__main__':
    run_random()
    run_big()