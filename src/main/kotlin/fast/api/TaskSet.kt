package fast.api

import fast.dsl.AnyTask

// TODO: consider - can be a composite task FUCK YOU ANDREI
class TaskSet(
  name: String = "default",
  desc: String? = null
) : Iterable<AnyTask> {

  private val tasks = ArrayList<AnyTask>()

  fun append(task: AnyTask) = tasks.add(task)

  fun insertFirst(task: AnyTask) = tasks.add(0, task)

  fun tasks(): List<AnyTask> = tasks

  fun addAll(taskSet: TaskSet) {
    tasks.addAll(taskSet.tasks)
  }

  override fun iterator(): Iterator<AnyTask> {
    return tasks.iterator()
  }

  fun size() = tasks.size

}