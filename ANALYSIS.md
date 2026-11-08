# Assignment 4 — analysis

## Environment and commands

- Language: Java. Java version: 25.0.1 (the launcher compiles with `--release 17`).
- Computer: MacBook Air, macOS, aarch64, 10 CPUs. **Apple M4, 16 GB RAM, macOS 15.5**
- All my code is in `Java/source/Algorithms.java`. I did not change `Main.java` or `Json.java`.
- Commands (run from the project folder):
    - `python3 tools/run.py --setup` (checks that the project compiles and starts)
    - `python3 tools/run.py --check` (44 public tests and a repeated batch)
    - `python3 tests/student/stress.py` (my own random tests and big tests)
    - `mkdir -p /tmp/bench`
    - `javac -cp Java/build -d /tmp/bench tests/student/Bench.java`
    - `java -cp Java/build:/tmp/bench Bench` (the speed comparison)
- I do not use any library for the algorithms. The only library function I use inside an algorithm is `Arrays.sort` on a `long[]` (in the suffix array).
## Algorithm explanations

### lcs
- **State:** `dp[i][j]` is the length of the longest common subsequence of the first `i` characters of `a` and the first `j` characters of `b`.
- **Base case:** `dp[0][j] = 0` and `dp[i][0] = 0`, because an empty string has nothing in common with anything. This also handles empty input.
- **Transition:** if `a[i-1] == b[j-1]`, then `dp[i][j] = dp[i-1][j-1] + 1`. If not, `dp[i][j] = max(dp[i-1][j], dp[i][j-1])` (we skip the last character of `a` or of `b`).
- **Order:** `i` from small to big, and for each `i`, `j` from small to big. Every cell only needs cells to the left, above or diagonal, and those are already filled.
- **Why it is correct:** if the last characters are equal, we can match them and look at the shorter prefixes. If they are different, at least one of them is not used in the answer, so we take the better of the two options.
- **Reconstruction:** start at `(|a|, |b|)` and go back. If the characters are equal, add the character and go diagonally. Otherwise go to the neighbour with the bigger value (on a tie I go up). At the end I reverse the string. Any correct longest sequence is accepted, so ties are not a problem.
- **Time and memory:** O(|a|·|b|) for both. The table is an `int[][]`, at most about 10^6 cells.
- **Test:** `a = ""`, `b = "abc"` gives length 0 and sequence `""`. It checks that empty input does not crash.
### merge
- **State:** `dp[l][r]` is the smallest total cost to merge blocks `l` to `r` into one block. `split[l][r]` remembers where the last merge happened.
- **Base case:** `dp[i][i] = 0`. If there are 0 or 1 blocks, the answer is cost 0 and an empty plan.
- **Transition:** the last merge joins `[l..k]` and `[k+1..r]`. Its cost is the sum of all sizes from `l` to `r` (I use prefix sums). So `dp[l][r] = min over k of (dp[l][k] + dp[k+1][r]) + sum(l..r)`.
- **Order:** by interval length (2, 3, 4, ...), so the smaller intervals are ready.
- **Why it is correct:** any merge order is a tree, and the last merge splits the blocks into a left part and a right part. The cost of the last merge does not depend on `k`, and the two parts do not affect each other, so we can make each part as cheap as possible.
- **Reconstruction:** a recursive function adds the plan of the left part, then the right part, then the merge `[l, k, r]`. So the small merges always come before the big merge that uses them. The depth is at most 200, so recursion is safe.
- **Time and memory:** O(n^3) time and O(n^2) memory. Sums are `long` because they can be bigger than an `int`.
- **Test:** sizes `[10^9, 10^9, 10^9]` give cost 5000000000. It checks that there is no integer overflow.
### tree
- **State:** `take[v]` is the best weight in the subtree of `v` if we take `v`. `skip[v]` is the best weight if we do not take `v`.
- **Base case:** for a leaf, `take = weight of v` and `skip = 0`.
- **Transition:** `take[v] = w[v] + sum of skip[child]` (children cannot be taken if `v` is taken). `skip[v] = sum of max(take[child], skip[child])`.
- **Order:** I do a BFS from vertex 0 and save the order. Then I go through this order backwards, so every child is processed before its parent. I do not use recursion, because a tree with 100000 vertices in one chain would crash the stack.
- **Why it is correct:** neighbours cannot both be chosen, so a chosen vertex forces its children to be skipped, and a skipped vertex lets each child choose freely. The answer is `max(take[root], skip[root])`. Choosing nothing is allowed, so the weight is never below 0.
- **Reconstruction:** I go through the BFS order from the top. A vertex is chosen if its parent is not chosen and `take[v] > skip[v]`. On a tie I skip it.
- **Time and memory:** O(n) for both. I store the edges in arrays and use `long` for sums.
- **Test:** weights `[-4, -1, -5]` on a path give weight 0 and an empty selected list. It checks that we do not force a non-empty answer.
### naive
- **Idea:** for each start position `i` from 0 to `n-m`, compare `text[i+j]` with `pattern[j]` one by one. If all `m` characters match, save `i`.
- **Last start:** `n - m`. For an empty pattern the starts are `0..n`, so `n+1` positions. If the pattern is longer than the text, the loop does not run and the answer is `[]`.
- **Overlaps:** every start is checked separately, so overlapping matches are found.
- **Time and memory:** O(n·m) time (plus the output). Only O(1) extra memory besides the result list.
- **Test:** text `aaaa`, pattern `aa` gives `[0, 1, 2]`. Text `""` and pattern `""` gives `[0]`.
### kmp
- **State:** `pi[i]` is the length of the longest proper prefix of `pattern[0..i]` that is also a suffix of it. While searching, `k` is how many characters of the pattern match right now.
- **Building `pi`:** for each `i`, start with `k = pi[i-1]`, go back with `k = pi[k-1]` while the characters are different, and add 1 if they are equal.
- **Searching:** if there is a mismatch, go back with `k = pi[k-1]`. If equal, `k++`. When `k == m` we found a match at `i-m+1`, and then I set `k = pi[m-1]`. This keeps overlapping matches (`abababa` with `aba` gives 0, 2, 4).
- **Why it is correct:** `k` is always the longest part of the pattern that is the end of the text read so far. The borders in `pi` are the only candidates for the next value of `k`, so no match is lost.
- **Time and memory:** O(n + m) (plus the output). `k` only grows by 1 per text character and every step back makes it smaller, so the total work is linear. Extra memory is O(m) for `pi`. An empty pattern is handled before using `pi`.
- **Test:** text `aaaa`, pattern `aa`. It checks that after a full match we go back through `pi` and do not restart (otherwise overlaps are lost).
### rk
- **Hash:** base 31, modulus 101, ASCII codes. The leftmost character has the biggest power: `hash = (s[0]*31^(m-1) + ... + s[m-1]) mod 101`.
- **Moving the window:** remove the left character, multiply by 31, add the new right character, all mod 101. The remainder can be negative, so I do `((x % 101) + 101) % 101`.
- **Checking:** if the hashes are equal, it is only a candidate. I compare the real characters and only then save the position.
- **Special cases:** empty pattern gives `0..n`. Pattern longer than text gives `[]`. Both are handled before making any window.
- **Why it is correct:** equal strings always have equal hashes, so no real match is skipped. The character check removes false matches.
- **Time and memory:** O(n + m + c·m), where `c` is the number of windows with equal hash. In the worst case this is O(n·m), so RK is not always linear. Extra memory is O(1) besides the result.
- **Test (collision):** `ab` and `dj` both have hash 75 (text `abdj`, pattern `ab` gives `[0]`). Also `bc` and `ek` both have hash 6 (text `bcek`, pattern `bc` gives `[0]`). It checks that we verify characters.
### suffix
**Prefix doubling (building the suffix array):**
- At the start, the rank of each suffix is the code of its first character.
- In every round I give each suffix a pair `(rank[i], rank[i+k] + 1)`. If `i+k` is outside the text, the second number is 0, so shorter suffixes come first. Then I sort by this pair. Suffixes with equal pairs get equal new ranks. Then `k` becomes `2k`.
- To make sorting simple, I put the pair and the index `i` into one `long` number and sort the `long[]` array. This works because n ≤ 50000 is smaller than 65536.
- I stop when all ranks are different. Then the sorted order is the suffix array `sa`.
- Why it works: the first `2k` characters are the first `k` characters plus the next `k` characters, and the order of both parts is already known from the ranks.
- Time: about log n rounds, each round sorts n numbers, so O(n log² n). Memory O(n). I never make strings for the suffixes.
  **Kasai (LCP array):**
- `rnk[sa[i]] = i` is the opposite of `sa`. I go through suffixes in text order and keep a number `h`. For suffix `i`, I compare it with the suffix just before it in `sa` and make `h` bigger while characters are equal. Then I save `lcp[rnk[i]] = h`.
- The main trick: for the next suffix `i+1`, the common part is at least `h-1`. So I only do `h--` and never start from 0. That makes the total work O(n).
- `lcp[0] = 0`, and `lcp[r]` compares `sa[r-1]` and `sa[r]`.
  **Longest repeated substring:** two equal substrings are a common beginning of two suffixes, and suffixes with a long common beginning are neighbours in `sa`. So the biggest value in `lcp` is the length of the longest repeat, and I cut that part from the text. If the biggest value is 0, the answer is `""`. Overlaps are allowed (`aaaa` gives `aaa`).

- **Tests:** empty text gives `sa=[]`, `lcp=[]`, `repeated=""`. Text `aaaa` gives `sa=[3,2,1,0]` and `lcp=[0,1,2,3]`.
### aho
- **Trie:** each pattern is added to a trie. `go[state*95 + (c-32)]` stores the child (the alphabet is ASCII 32..126, so 95 characters). Duplicate patterns end in the same state, but each one keeps its own position in the dictionary, so duplicates get equal counts.
- **Failure links:** I use BFS. The failure link of a state points to the longest shorter ending of its string that is also in the trie. I also fill the missing transitions with the transitions of the failure link, so the search never needs a loop of failure jumps.
- **Counting in one text:** for every character I move to the next state and do `vis[state]++`. After the text I go through the states in reverse BFS order and add `vis[u]` to `vis[fail[u]]`. This way a pattern that is the end of a longer pattern is also counted (`he` inside `she`), and overlaps are counted too, without listing every match. The answer for pattern `j` is `vis[terminal state of j]`.
- **Reset:** `vis` is a new array for every text, so texts do not affect each other.
- **Special cases:** empty dictionary gives an empty row for every text. No texts gives `[]`. An empty text gives a row of zeros.
- **Time and memory:** with `L` = total length of patterns, `S` = number of states, `p` = number of patterns: building takes O(L + 95·S + p) time and O(95·S + p) memory. Each text takes O(|text| + S + p).
- **Tests:** patterns `[cat, at]` and text `cat` give `[1, 1]` (checks suffix patterns). Patterns `[a, aa, a]` and text `aaa` give `[3, 2, 3]` (checks overlaps and duplicates).
## Own tests

I used the 44 public tests (`tests/public.json`) and my own script `tests/student/stress.py`. The script makes random small inputs and compares my answers with simple slow solutions (brute force): all merge orders for `merge`, all subsets for `tree`, `startswith` for the searches, sorting real suffix strings for `suffix`, and counting with `startswith` for `aho`. It checks answers with the same validator as `run.py`, so a different correct answer is also accepted. It also runs inputs of maximum size.

|Command|Input or fixture path|Expected result|Actual result / log|What it checks|
|---|---|---|---|---|
|all|`tests/public.json` with `python3 tools/run.py --check`|all 44 pass|44 PASS and `PASS state reset: two complete batches in one process`|examples, empty inputs, ties, overlaps, collision, reset between requests|
|all 8 tasks|`stress.py`, seed 1, 300 random cases per task (2400 requests)|same as brute force|`random: 2400 cases, 0 failures`|random small inputs, ties, empty inputs|
|tree|chain of 100000 vertices, all weights 1|weight 50000|ok=True, 0.11 s|no stack overflow|
|kmp|text `a`×200000, pattern `a`×100000|positions 0..100000|ok=True, 0.06 s|overlaps, maximum size|
|naive|text `a`×3000, pattern `a`×1500|positions 0..1500|ok=True, 0.04 s|many overlapping matches|
|rk|text `a`×10000, pattern `a`×2000|positions 0..8000|ok=True, 0.05 s|every window is checked (worst case)|
|suffix|text `a`×50000|`sa` = 49999..0, `lcp` = 0..49999|ok=True, 0.07 s|maximum size, many equal ranks|
|lcs|two random strings of length 1000 (seed 7)|same length as reference|ok=True, 0.03 s|maximum size|
|merge|200 random sizes up to 10^9|valid plan|ran in 0.03 s (no reference at this size)|speed and big numbers; correctness is checked by the small random tests|
|aho|10000 times `a`×10 as patterns, 100 texts of `a`×2000|every count is 1991|ok=True, 0.13 s|duplicates, many matches, many texts|

One mistake I found in my own test code: the first run of `stress.py` showed 8 failures, all for `suffix` with an empty text. The problem was in my brute-force reference (it gave `lcp = [0]` for an empty text, but the task says `lcp = []`). My solution was right. After I fixed the reference, the result was `random: 2400 cases, 0 failures`.

## Search comparison

**What I did.** I gave the same text and pattern to `naive`, `kmp` and `rk` (all with n, m ≤ 3000). Before timing, I checked that all three give the same answer (last column). Random case: text from the letters `abcd`, `java.util.Random` with seed 12345, and the pattern is a random part of the text (so it exists in the text). Repetitive case: text is `a` repeated n times, pattern is `a` repeated m-1 times and then `b`.

**How I measured.** The number is the average time of one call in microseconds (µs), taken over 300 calls in a row with `System.nanoTime`. It includes making the result list, but not reading JSON, starting the program or compiling. For each algorithm and case, I first did 5 warm-up runs (300 calls each) and threw them away because of Java's JIT warm-up. Then I measured 3 runs. The median is the middle one of the 3 values. Java 25.0.1, macOS aarch64, 10 CPUs. (The program printed decimal commas, I wrote them as dots.)

|Case|n|m|Seed/generator|Algorithm|Run 1 (µs/call)|Run 2|Run 3|Median|Correct outputs checked|
|---|---:|---:|---|---|---:|---:|---:|---:|---|
|random|500|10|seed 12345, letters abcd, pattern = part of text|naive|1.0|1.0|1.0|1.0|yes, all 3 agree|
|random|500|10|same|kmp|6.6|6.8|6.6|6.6|yes, all 3 agree|
|random|500|10|same|rk|7.3|7.3|7.5|7.3|yes, all 3 agree|
|random|1000|50|same|naive|1.5|1.5|1.5|1.5|yes, all 3 agree|
|random|1000|50|same|kmp|1.9|2.0|1.9|1.9|yes, all 3 agree|
|random|1000|50|same|rk|8.8|8.7|8.7|8.7|yes, all 3 agree|
|random|3000|200|same|naive|4.1|4.2|4.2|4.2|yes, all 3 agree|
|random|3000|200|same|kmp|5.6|5.5|5.5|5.5|yes, all 3 agree|
|random|3000|200|same|rk|26.2|26.3|26.2|26.2|yes, all 3 agree|
|repetitive|500|10|text a^n, pattern a^(m-1)b|naive|4.4|4.4|4.4|4.4|yes, all 3 agree|
|repetitive|500|10|same|kmp|1.7|1.7|1.7|1.7|yes, all 3 agree|
|repetitive|500|10|same|rk|4.4|4.4|4.4|4.4|yes, all 3 agree|
|repetitive|1000|50|same|naive|41.0|41.2|41.0|41.0|yes, all 3 agree|
|repetitive|1000|50|same|kmp|3.4|3.4|3.4|3.4|yes, all 3 agree|
|repetitive|1000|50|same|rk|8.6|8.7|8.6|8.6|yes, all 3 agree|
|repetitive|3000|200|same|naive|506.1|503.8|503.5|503.8|yes, all 3 agree|
|repetitive|3000|200|same|kmp|10.3|10.4|10.3|10.3|yes, all 3 agree|
|repetitive|3000|200|same|rk|25.7|25.7|25.7|25.7|yes, all 3 agree|

**What I see.** These times are only for my computer and these inputs. They do not prove the complexity.

- **Repetitive text:** naive gets much slower when n and m grow (4.4, 41.0, 503.8 µs). This fits O(n·m), because almost every start matches many characters before the last `b` fails. KMP and RK grow slowly (KMP: 1.7, 3.4, 10.3 µs). At the biggest size KMP is about 49 times faster than naive.
- **RK on repetitive text:** the hash of the window `a...a` and of the pattern `a...ab` always differ by 1 (mod 101), so RK never needed to check characters here. So this test does not show the bad worst case of RK. I tested the worst case separately in the own tests (text `a`×10000, pattern `a`×2000, every window is checked, 0.05 s).
- **Random text:** naive is fastest (1.0, 1.5, 4.2 µs), because it usually fails on the first or second character. RK is the slowest (7.3, 8.7, 26.2 µs). I think the reasons are the extra modulo calculation for every window and about 1 out of 101 windows having the same hash by chance (so they need a check), but I did not measure this, so it is only my guess.
- **Strange value:** KMP on random 500/10 (6.6 µs) is slower than KMP on random 1000/50 (1.9 µs). I think it is the JIT warm-up (it was the first KMP case I measured), but I did not check this.
- **Small modulus 101:** there are only 101 possible hash values, so different windows often get the same hash (`ab` and `dj` both give 75, `bc` and `ek` both give 6). The algorithm is still correct only because I compare the characters when the hashes are equal. 101 is small on purpose, to make collisions easy to test. Real programs use much bigger moduli, so this does not describe every real hash implementation. The two collision examples above are in the correctness tests.
## Sources and AI assistance

- Sources: the assignment text (`docs/Assignment_4_EN.md`), the lecture materials and the public tests and launcher from the repository.
- **AI tool.** It explained the algorithms step by step and helped me write the Java code in `Algorithms.java`, the test script `stress.py`, the benchmark `Bench.java` and a draft of this document.
- What I checked myself: I ran `python3 tools/run.py --check` (all 44 public tests pass), ran my random tests against brute-force solutions (2400 requests, 0 failures after I fixed a mistake in my own reference code), ran the maximum-size tests, and ran the benchmark on my computer. All numbers in this document are from these runs. **I created my repository from the template, set the language to Java and put the code into `Java/source/Algorithms.java`. I ran `python3 tools/run.py --setup` and `python3 tools/run.py --check` myself. At first only suffix and aho failed because they were not written yet; after I added them, all 44 public tests and the state-reset check passed. I ran `tests/student/stress.py` and found that the 8 failures in the first run came from a mistake in the brute-force reference (it returned `lcp = [0]` for an empty text), not from my solution. I fixed this line and the second run gave 0 failures. I also ran the benchmark `Bench.java` on my own MacBook, and the numbers in the table are copied from that output.**