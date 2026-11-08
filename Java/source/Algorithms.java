import java.util.*;
/** Implement these algorithms. docs/guide/CONTRACTS.md defines each field and limit. */
public final class Algorithms {
    static Map<String,Object> result(Object... kv) {
        Map<String,Object> r = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }

    static Map<String,Object> allBoundaries(int n) {
        List<Integer> pos = new ArrayList<>();
        for (int i = 0; i <= n; i++) pos.add(i);
        return result("positions", pos);
    }
    /** Compare two versions of a sequence while preserving the order of shared characters. Implement lcs according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> lcs(Map<String,Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++)
                if (a.charAt(i - 1) == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);

        StringBuilder sb = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { sb.append(a.charAt(i - 1)); i--; j--; }
            else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
            else j--;
        }
        return result("length", dp[n][m], "sequence", sb.reverse().toString());
    }
    /** Plan the cheapest sequence of adjacent block merges when the original order must be preserved. Implement merge according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> merge(Map<String,Object> input) {
        List<?> raw = (List<?>) input.get("sizes");
        int n = raw.size();
        if (n <= 1) return result("cost", 0L, "plan", new ArrayList<Object>());
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + (Long) raw.get(i);
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; len++)
            for (int l = 0; l + len - 1 < n; l++) {
                int r = l + len - 1;
                long best = Long.MAX_VALUE;
                for (int k = l; k < r; k++) {
                    long c = dp[l][k] + dp[k + 1][r];
                    if (c < best) { best = c; split[l][r] = k; }
                }
                dp[l][r] = best + pre[r + 1] - pre[l];
            }
        List<Object> plan = new ArrayList<>();
        buildPlan(0, n - 1, split, plan);
        return result("cost", dp[0][n - 1], "plan", plan);
    }

    static void buildPlan(int l, int r, int[][] split, List<Object> plan) {
        if (l >= r) return;
        int k = split[l][r];
        buildPlan(l, k, split, plan);
        buildPlan(k + 1, r, split, plan);
        plan.add(new int[]{l, k, r});
    }
    /** Choose a maximum-value set when neighbouring vertices are incompatible and conflicts form a tree. Implement tree according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> tree(Map<String,Object> input) {
        int n = ((Long) input.get("n")).intValue();
        if (n == 0) return result("weight", 0L, "selected", new ArrayList<Object>());
        List<?> w = (List<?>) input.get("weights");
        List<?> edges = (List<?>) input.get("edges");

        int[] head = new int[n], nxt = new int[2 * (n - 1)], to = new int[2 * (n - 1)];
        Arrays.fill(head, -1);
        int cnt = 0;
        for (Object eo : edges) {
            List<?> e = (List<?>) eo;
            int u = ((Long) e.get(0)).intValue(), v = ((Long) e.get(1)).intValue();
            to[cnt] = v; nxt[cnt] = head[u]; head[u] = cnt++;
            to[cnt] = u; nxt[cnt] = head[v]; head[v] = cnt++;
        }
        int[] parent = new int[n], order = new int[n];
        Arrays.fill(parent, -2);
        parent[0] = -1;
        int qh = 0, qt = 0;
        order[qt++] = 0;
        while (qh < qt) {
            int v = order[qh++];
            for (int e = head[v]; e != -1; e = nxt[e]) {
                int u = to[e];
                if (parent[u] == -2) { parent[u] = v; order[qt++] = u; }
            }
        }
        long[] take = new long[n], skip = new long[n];
        for (int i = 0; i < n; i++) take[i] = (Long) w.get(i);
        for (int i = n - 1; i > 0; i--) {
            int v = order[i], p = parent[v];
            take[p] += skip[v];
            skip[p] += Math.max(take[v], skip[v]);
        }
        boolean[] chosen = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int v = order[i], p = parent[v];
            boolean parentTaken = p >= 0 && chosen[p];
            if (!parentTaken && take[v] > skip[v]) { chosen[v] = true; selected.add(v); }
        }
        return result("weight", Math.max(take[0], skip[0]), "selected", selected);
    }
    /** Establish a simple exact-search baseline against which the other two searches can be checked. Implement naive according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> naive(Map<String,Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
            if (j == m) pos.add(i);
        }
        return result("positions", pos);
    }
    /** Reuse information about matched prefixes to avoid restarting the search after every mismatch. Implement kmp according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> kmp(Map<String,Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        if (m == 0) return allBoundaries(n);
        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int k = pi[i - 1];
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        List<Integer> pos = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < n; i++) {
            while (k > 0 && t.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (t.charAt(i) == p.charAt(k)) k++;
            if (k == m) { pos.add(i - m + 1); k = pi[k - 1]; }
        }
        return result("positions", pos);
    }
    /** Reject windows with unequal fingerprints and verify every equal-hash candidate. Implement rk according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> rk(Map<String,Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        if (m == 0) return allBoundaries(n);
        List<Integer> pos = new ArrayList<>();
        if (m > n) return result("positions", pos);
        final long B = 31, M = 101;
        long pow = 1;
        for (int i = 0; i < m - 1; i++) pow = pow * B % M;
        long hp = 0, ht = 0;
        for (int i = 0; i < m; i++) {
            hp = (hp * B + p.charAt(i)) % M;
            ht = (ht * B + t.charAt(i)) % M;
        }
        for (int i = 0; i + m <= n; i++) {
            if (hp == ht) {
                int j = 0;
                while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
                if (j == m) pos.add(i);
            }
            if (i + m < n) {
                ht = ((ht - t.charAt(i) * pow) % M + M) % M;
                ht = (ht * B + t.charAt(i + m)) % M;
            }
        }
        return result("positions", pos);
    }
    /** Order the text suffixes and use adjacent common prefixes to find repeated substrings. Implement suffix according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> suffix(Map<String,Object> input) {
        String s = (String) input.get("text");
        int n = s.length();
        if (n == 0) return result("sa", new ArrayList<Object>(), "lcp", new ArrayList<Object>(), "repeated", "");

        int[] sa = new int[n], rank = new int[n], tmp = new int[n];
        for (int i = 0; i < n; i++) rank[i] = s.charAt(i);
        long[] keys = new long[n];
        long base = Math.max(n, 127) + 2;

        for (int k = 1; ; k <<= 1) {
            for (int i = 0; i < n; i++) {
                int r2 = (i + k < n) ? rank[i + k] + 1 : 0;
                keys[i] = ((long) rank[i] * base + r2) * 65536L + i;
            }
            Arrays.sort(keys);
            for (int i = 0; i < n; i++) sa[i] = (int) (keys[i] & 65535L);
            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++)
                tmp[sa[i]] = tmp[sa[i - 1]] + ((keys[i] >>> 16) != (keys[i - 1] >>> 16) ? 1 : 0);
            System.arraycopy(tmp, 0, rank, 0, n);
            if (rank[sa[n - 1]] == n - 1) break;
        }

        int[] rnk = new int[n];
        for (int i = 0; i < n; i++) rnk[sa[i]] = i;
        int[] lcp = new int[n];
        int h = 0;
        for (int i = 0; i < n; i++) {
            if (rnk[i] > 0) {
                int j = sa[rnk[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                lcp[rnk[i]] = h;
                if (h > 0) h--;
            } else h = 0;
        }

        int bestLen = 0, bestIdx = 0;
        for (int i = 1; i < n; i++) if (lcp[i] > bestLen) { bestLen = lcp[i]; bestIdx = i; }
        String repeated = bestLen == 0 ? "" : s.substring(sa[bestIdx], sa[bestIdx] + bestLen);
        return result("sa", sa, "lcp", lcp, "repeated", repeated);
    }
    /** Build one dictionary automaton and count its patterns in several independent texts. Implement aho according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> aho(Map<String,Object> input) {
        List<?> pats = (List<?>) input.get("patterns");
        List<?> texts = (List<?>) input.get("texts");
        int p = pats.size();
        final int A = 95;

        int total = 1;
        for (Object o : pats) total += ((String) o).length();
        int[] go = new int[total * A];
        int[] fail = new int[total];
        int[] term = new int[p];
        int states = 1;
        for (int j = 0; j < p; j++) {
            String pat = (String) pats.get(j);
            int v = 0;
            for (int i = 0; i < pat.length(); i++) {
                int c = pat.charAt(i) - 32;
                if (go[v * A + c] == 0) go[v * A + c] = states++;
                v = go[v * A + c];
            }
            term[j] = v;
        }

        int[] order = new int[states];
        int qh = 0, qt = 0;
        order[qt++] = 0;
        while (qh < qt) {
            int v = order[qh++];
            for (int c = 0; c < A; c++) {
                int u = go[v * A + c];
                if (u != 0) {
                    fail[u] = (v == 0) ? 0 : go[fail[v] * A + c];
                    order[qt++] = u;
                } else {
                    go[v * A + c] = (v == 0) ? 0 : go[fail[v] * A + c];
                }
            }
        }

        List<Object> counts = new ArrayList<>();
        for (Object to : texts) {
            String t = (String) to;
            long[] vis = new long[states];
            int v = 0;
            for (int i = 0; i < t.length(); i++) {
                v = go[v * A + (t.charAt(i) - 32)];
                vis[v]++;
            }
            for (int i = states - 1; i > 0; i--) {
                int u = order[i];
                vis[fail[u]] += vis[u];
            }
            long[] row = new long[p];
            for (int j = 0; j < p; j++) row[j] = vis[term[j]];
            counts.add(row);
        }
        return result("counts", counts);
    }
}
