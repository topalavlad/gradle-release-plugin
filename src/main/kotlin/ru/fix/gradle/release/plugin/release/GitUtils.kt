package ru.fix.gradle.release.plugin.release

import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.logging.Logging
import ru.fix.gradle.release.plugin.release.GitHolder.git

class GitUtils {
    companion object {


        const val GIT_LOGIN_PARAMETER = "git.login"
        const val GIT_PASSWORD_PARAMETER = "git.password"


        private val logger = Logging.getLogger("GitUtils")!!

        fun getCurrentBranch(): String = GitHolder.git.repository.branch

        fun isBranchExists(branch: String): Boolean {


            return GitHolder.git.branchList().call()
                    .stream().filter { branch == it.name }
                    .findAny().isPresent
        }

        fun createBranch(branch: String, checkout: Boolean = false): Ref {


            logger.lifecycle("Creating branch $branch ${if (checkout) "and checkout" else ""}")
            val ref = GitHolder.git.branchCreate().setName(branch).call()
            return if (checkout) {
                GitHolder.git
                        .checkout()
                        .setName(branch)
                        .call()
            } else {
                ref
            }
        }

        fun checkoutBranch(branch: String): Ref {
            logger.lifecycle("Checkout $branch branch")
            return GitHolder.git.checkout()
                    .setName(branch).call()
        }

        fun checkoutTag(tag: String) {
            logger.lifecycle("Checkout $tag tag")
            GitHolder.git.checkout()
                    .setCreateBranch(true)
                    .setName("tags/$tag")
                    .call()
        }


        fun createTag(name: String, comment: String): Ref {

            logger.lifecycle("Creating tag $name with comment $comment")


            return GitHolder.git
                    .tag()
                    .setAnnotated(true)
                    .setName(name)
                    .setMessage(comment)
                    .call()
        }


        fun deleteBranch(name: String) {
            logger.lifecycle("Deleting branch $name")
            GitHolder.git
                    .branchDelete()
                    .setBranchNames(name)
                    .setForce(true)
                    .call()
        }


        fun commitFilesInIndex(commitMessage: String) {
            logger.lifecycle("Commiting files")

            GitHolder.git.add().addFilepattern(".")
                    .call()
            git.commit()
                    .setMessage(commitMessage)
                    .call()

        }

        fun pushTag(userName: String, password: String, tagRef: Ref) {
            logger.lifecycle("Pushing tag $tagRef to remote repository")

            val pushCommand = GitHolder.git.push()

            with(pushCommand) {
                add(tagRef)
                setCredentialsProvider(UsernamePasswordCredentialsProvider(userName, password))
                call()
            }

        }

    }


}