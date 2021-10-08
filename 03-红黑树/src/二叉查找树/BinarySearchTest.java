package 二叉查找树;

/**
 * @author zhoudbw
 * 二分查找算法
 */
public class BinarySearchTest {
    public static void main(String[] args) {
        int[] arr = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        System.out.println(binarySearch(arr, 20));
    }

    /**
     * 二分查找算法
     *
     * @param arr  有序数组
     * @param data 查找的数据
     * @return index 下标，未查找到时返回-1
     */
    public static int binarySearch(int[] arr, int data) {
        int low = 0;
        int high = arr.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < data) {
                low = mid + 1;
            } else if (arr[mid] == data) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }
}
