# Gradle Release Plugin
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/gradle-release-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

gradle-release-plugin automates release procedure for gradle based projects. 
It automatically creates release branches, update project version in root `gradle.properties` file 
and commit this update in dynamically created tag with autoincremented version.

# Usages

## Short hints
Create minor fix/feature in same release branch, e.g. tag 1.3.5 -> tag 1.3.6
```
git checkout releases/release-1.3 
gradle createRelease

#optional, plugin by default tries to push changes by itself if ssh/https keys/credentials provided
git push --tags
```
Create new major version in a new branch, e.g. release-1.3 -> release-1.4
```
git checkout releases/release-1.3 
gradle createReleaseBranch
git push
```

## Plugin tasks

### createReleaseBranch
Creates new branch in local git repository. 
By default base branch name is `/master` and target branch name is `/release-x.y`. 
During execution command asks user to specify major and minor version `x.y`
 
```
#before
└─master
└─releases
  └─release-1.0
  └─release-1.1
└─features
  └─my-new-future

gradle createReleaseBranch
> 1.2

#after
└─master
└─releases
  └─release-1.0
  └─release-1.1
  └─release-1.2  <--
└─features
  └─my-new-future
``` 
 
## createRelease 
Searches for existing tags in repository that name matches version template `x.y.z`.    
Finds latest one.    
Calculates new version by incrementing latest one.    
Stores new version in `gradle.properties` file.     
Commit `gradle.properties` file with new tag name `x.y.z+1` into repository.  
User should run createRelease task on one of release branches.  
For https connection use parameters: `-Pgit.login=<git.login>` `-Pgit.password=<git.password>`
No credentials required in case of ssh key.
    
Configuration:
 * mainBranch: String - base branch name, by default - `/master`
 * releaseBranchPrefix: String - prefix for release branch, by default - `/releases/release-`

```
#before
└─master
└─releases
  └─release-1.0
  └─release-1.1
tag 1.0.1
tag 1.1.1
tag 1.1.2
tag 1.1.3

git checkout -b releases/release-1.1 
gradle createRelease

#after
└─master
└─releases
  └─release-1.0
  └─release-1.1
tag 1.0.1
tag 1.1.1
tag 1.1.2
tag 1.1.3  <--
``` 

### How to use plugin

Add plugin to project gradle build script
```
import org.gradle.kotlin.dsl.*
import ru.fix.gradle.release.plugin.release.ReleaseExtension

buildscript{
    dependencies {
        classpath("ru.fix:gradle-release-plugin:$version")
    }
}

apply {
    plugin("ru.fix.gradle.release")
}

//not required for default configuration
configure<ReleaseExtension> {
    mainBranch = "master"
    releaseBranchPrefix = "releases/release-"
}
```
Manually create branch `/master` with latest version of project.  
```
└─master
```
Create new release branch with gradle plugin `gradle createReleaseBranch`  and specify version `1.0`.
```
gradle createReleaseBranch
> 1.0

└─master
└─releases
  └─release-1.0  <--
```  
This will create new branch `/master` -> `/releases/release-1.0`  
Checkout branch `/releases/release-1.1`  
Apply changes. 
Create new tag with gradle plugin `gradle createRelease`
```
gradle createRelease

└─master
└─releases
  └─release-1.0
tag 1.0.1  <--
```  
This will create new tag `1.0.1`

Now CI can checkout tag `1.0.1`, build and publish your project `gradle clean build publish`.

You can commit fixes to `/release/release-1.0` and create new tag:  
`gradle createRelease` will create new tag `1.0.2`.
```
gradle createRelease

└─master
└─releases
  └─release-1.0
tag 1.0.1
tag 1.0.2  <--
```  
If you decided to publish new version based on `/master` branch you can create new release
branch `gradle createReleaseBranch` and specify version `1.1`.  
This will create new branch `/master` -> `/releases/release-1.1`
```
gradle createReleaseBreanch

└─master
└─releases
  └─release-1.0
  └─release-1.1  <--
tag 1.0.1
tag 1.0.2
```  

## Release flow
### Principles
- Latest stable functionality is located in `/master` branch
- New features is located in `/features/feature-name` branches
- Maintainable releases is located in `/releases/release-x.y` branches
- Project version is specified in root file `gradle.properties`, field `version=x.y.z`
- Version in `gradle.properties` file in all branches is committed as `x.y-SNAPSHOT`. 
 This will prevent conflicts during merge requests between feature branches `/features/*`,
 release branches `/releases/release-x.y` and `/master` branch
- During release new tags is being created that holds single commit that modifies `gradle.properties` file 
and specify particular `version=x.y.z`

### Release procedure
Suppose that we already have last version of project in `/master` branch, and release `/releases/release-1.2`
- New branch is created based on `/master`. 
Branch name is `/releases/release-1.3`. 
Plugin task `createReleaseBranch` could be used for that purpose.
- New branch `/releases/release-1.3` is stabilized, and changes is added through Merge Requests.
- When branch `/releases/release-1.3` is ready, user launches CI build server release task and specify given branch
 `/releases/release-1.3`
- CI build server task checkout `/releases/release-1.3` branch, then executes gradle command `gradle createRelease`
- gradle plugin searches in local git repository for all tags that matches `1.3.*` template, if there is no such 
tag found 
then default `1.3.1` will be used. Otherwise max tag will be incremented, e.g. if plugin find `1.3.7` then new tag 
name will be `1.3.8`  
- In file `gradle.properties` `version` property is replaced from `1.y-SNAPSHOT` to `1.3.8`
- `gradle.properties` is being committed with new tag name `1.3.8`


# Project template
Common project configuration requires properties provided through `gradle.properties` or environment:
```properties
repositoryUrl= <https://path/to/remote/repository> or <file:///path/to/local/repository>
repositoryUser= login for remote repository
repositoryPassword= password for remote repository
signingKeyId= gpg short keyid, could be obtained via gpg --list-keys --keyid-format short
signingPassword= key store file password
signingSecretKeyRingFile= /path/to/.gnupg/secring.gpg key store file
  
Use `gpg --export-secret-keys -o secring.gpg` to export secret key to old format supported by gradle
```  
# Travis and Maven Central

To deploy project to maven central you have 
 - create account on sonatype
 - generate private key and sign artifacts before publication 
 
Generating private key.  
Export private key to old format: `secring.gpg`   
`gpg --export-secret-keys -o secring.gpg`

Encrypt `secring.gpg` and add it to your project repository `secring.gpg.enc`
```
travis encrypt-file secring.gpg 
``` 
Add decoding script into setup section of `.travis.yml`
```
jobs:
  include:
    - stage: build
      ...
      before_script: if [[ $encrypted_0cj38rd_key ]]; then openssl aes-256-cbc -K $encrypted_0cj38rd_key...

```

Encrypt project properties and add to secure section of `.travis.yml`
```
travis encrypt repositoryUrl=https://path/to/remote/repository
travis encrypt signingPassword=30cDKf34rdsl
...
```
```
env:
  global:
  - signingSecretKeyRingFile="`pwd`/secring.gpg"
  - secure: "MpiifWpBpsDfZ4OnQna/yRD4JaKXr9VvPXT4Ik0Njc/6y3BBGOsytXj4

```

## Generate .travis.yml
Script 
- encrypt gradle properties
- encrypt secring.gpg key store
- generate `.travis.yml` tempalte

`jfix-github-project-template/jfix-github-project-tempalte.py`



# Gradle Release Plugin project details    
## How to build
To build and deploy gradle release plugin project to local maven repository run:
```
gradle build install
```

### Deploy to remote repository
Provide credentials for repository.  
Provide signature.  
```
~/.gradle/gradle.properties

repositoryUser = user
repositoryPassword = password
repositoryUrl = url-to-repository
signingKeyId = key id 8 letters long (gpg --list-keys --keyid-format short)
signingPassword = passowrd to acces secring
signingSecretKeyRingFile = /home/user/.gnupg/secring.gpg
```
Specify version in
gradle.properties
```
version=x.y.z
```
commit new tag with name x.y.z  
then run
```
gradle build publish
```
