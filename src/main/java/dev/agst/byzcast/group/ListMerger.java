package dev.agst.byzcast.group;

import java.util.ArrayList;
import java.util.List;

class ListMerger {
  public static List<List<Integer>> mergeByLargestCommonSubsequence(List<List<Integer>> lists) {
    List<List<Integer>> mergedLists = new ArrayList<>();

    for (List<Integer> list : lists) {
      boolean isSublist = false;
      for (List<Integer> mergedList : mergedLists) {
        if (isSubsequence(mergedList, list)) {
          isSublist = true;
          break;
        } else if (isSubsequence(list, mergedList)) {
          mergedLists.remove(mergedList);
          mergedLists.add(list);
          isSublist = true;
          break;
        }
      }
      if (!isSublist) {
        mergedLists.add(list);
      }
    }

    return mergedLists;
  }

  private static boolean isSubsequence(List<Integer> parent, List<Integer> sublist) {
    if (sublist.size() > parent.size()) {
      return false;
    }
    int i = 0;
    for (int j = 0; j < parent.size() && i < sublist.size(); j++) {
      if (parent.get(j).equals(sublist.get(i))) {
        i++;
      }
    }
    return i == sublist.size();
  }
}
