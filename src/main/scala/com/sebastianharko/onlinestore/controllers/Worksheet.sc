

val v: Vector[Int] = Vector(1, 2, 3, 4, 2, 3)

v.toSet.subsets().map(_.toList).toList

for (i <- 0 to 5) {
  println(i)
}
val b = Vector(1, 2, 5)


def solutions(b: Vector[Int], i: Int, j: Int, solution: Array[Int]): Unit = {
  if (i == b.length) {
    println(solution.mkString(","))
  } else {
    if (j <= b(i)) {
      solution(i) = j
      solutions(b, i + 1, 0, solution)
      solutions(b, i, j + 1, solution)
    }
  }

}

solutions(b, 0, 0, Array.ofDim[Int](b.length))
