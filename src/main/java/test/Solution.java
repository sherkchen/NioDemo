package test;

import java.util.PriorityQueue;

public class Solution {
    public static void main(String[] args) {
        int[] gifts = {25,64,9,4,100};
        int k = 4;
        pickGifts(gifts, k);
    }

    // 大根堆
    public static long pickGifts(int[] gifts, int k) {
        PriorityQueue<Integer> queue = new PriorityQueue<>((a, b) -> a - b);
        for (int gift : gifts) {
            queue.add(gift);
        }
        while (k > 0) {
            int gift = queue.poll();
            int remain = (int)(Math.sqrt(gift));
            queue.add(remain);
            k--;
        }
        long ans = 0;
        while(!queue.isEmpty()) {
            ans += queue.poll();
        }
        return ans;

    }
}
