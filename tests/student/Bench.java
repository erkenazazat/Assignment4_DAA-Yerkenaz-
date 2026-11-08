import java.util.*;

/** Benchmark naive / KMP / RK on identical inputs. Run after Java/build exists. */
public class Bench {
    interface F { Map<String,Object> run(Map<String,Object> m); }
    static final int REPS = 300, WARMUP_ROUNDS = 5, RUNS = 3;
    static final long SEED = 12345;
    static long sink = 0;

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version") + ", " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch") + ", cpus=" + Runtime.getRuntime().availableProcessors());
        String[] names = {"naive", "kmp", "rk"};
        F[] fs = {Algorithms::naive, Algorithms::kmp, Algorithms::rk};
        int[][] sizes = {{500, 10}, {1000, 50}, {3000, 200}};
        Random rnd = new Random(SEED);
        System.out.println("|Case|n|m|Seed/generator|Algorithm|Run 1 (us/call)|Run 2|Run 3|Median|Correct outputs checked|");
        System.out.println("|---|---:|---:|---|---|---:|---:|---:|---:|---|");
        for (String kind : new String[]{"random", "repetitive"}) {
            for (int[] sm : sizes) {
                int n = sm[0], m = sm[1];
                String text, pattern;
                String gen;
                if (kind.equals("random")) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < n; i++) sb.append("abcd".charAt(rnd.nextInt(4)));
                    text = sb.toString();
                    int off = rnd.nextInt(n - m + 1);
                    pattern = text.substring(off, off + m);
                    gen = "seed " + SEED + ", alphabet abcd, pattern = random substring of text";
                } else {
                    text = "a".repeat(n);
                    pattern = "a".repeat(m - 1) + "b";
                    gen = "text a^n, pattern a^(m-1)b";
                }
                Map<String,Object> in = new HashMap<>();
                in.put("text", text); in.put("pattern", pattern);
                // correctness first: all three must agree
                Object ref = fs[0].run(in).get("positions");
                boolean same = true;
                for (F f : fs) same &= ref.equals(f.run(in).get("positions"));
                for (int a = 0; a < 3; a++) {
                    for (int w = 0; w < WARMUP_ROUNDS; w++) timeOnce(fs[a], in);
                    double[] t = new double[RUNS];
                    for (int r = 0; r < RUNS; r++) t[r] = timeOnce(fs[a], in);
                    double[] sorted = t.clone(); Arrays.sort(sorted);
                    System.out.printf("|%s|%d|%d|%s|%s|%.1f|%.1f|%.1f|%.1f|%s|%n", kind, n, m, gen, names[a],
                            t[0], t[1], t[2], sorted[RUNS / 2], same ? "yes (all 3 agree)" : "NO - MISMATCH");
                }
            }
        }
        System.err.println("sink=" + sink);
    }

    /** Average microseconds per call over REPS calls (excludes JSON parsing and compilation). */
    static double timeOnce(F f, Map<String,Object> in) {
        long t0 = System.nanoTime();
        for (int i = 0; i < REPS; i++) sink += ((List<?>) f.run(in).get("positions")).size();
        return (System.nanoTime() - t0) / 1e3 / REPS;
    }
}