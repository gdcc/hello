# Contributing Guide

## Building

```
mvn clean compile assembly:single
```

## Formatting code

```
mvn spotless:apply
```

## Making a release

Generally, see https://guides.dataverse.org/en/latest/developers/making-library-releases.html but below we'll use this repo as an example. The idea with this "hello" repo (and the "org.dataverse.test" namespace) is that you can practice both snapshot and real releases to Maven Central.

### Publish snapshot from local environment

We have a GitHub Action set up for publishing snapshots at [.github/workflows/maven-snapshot.yml](.github/workflows/maven-snapshot.yml) but first, let's try publishing a snapshot from your local environment.

A snapshot will be published with the version in pom.xml ends with `-SNAPSHOT`. If `-SNAPSHOT` is missing, bump the version number and add it. For example, if the version is "0.0.1", change it to "0.0.2-SNAPSHOT".

Create or edit `~/.m2/settings.xml` to contain the following:

```
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>REDACTED</username>
      <password>REDACTED</password>
    </server>
  </servers>
</settings>
```

For the username and password, use `DATAVERSEBOT_SONATYPE_USERNAME` and `DATAVERSEBOT_SONATYPE_TOKEN`, respectively, that the GitHub Action will use.

Build the snapshot:

```
mvn clean verify
```

Publish the snapshot:

```
mvn deploy
```

Verify the snapshot was uploaded:

tl;dr: Look for output like https://central.sonatype.com/repository/maven-snapshots/org/dataverse/test/hello/0.0.5-SNAPSHOT/hello-0.0.5-20250718.191054-2.jar in the publish step above and try to download that jar.

Before OSSRH was retired, you could browse through snapshot jars you published at <https://s01.oss.sonatype.org/content/repositories/snapshots/org/dataverse/test/hello/>, for example. Now, even though you may see the URL of the jar as shown above during the "publish" step, if you try to browse the various snapshot jars at https://central.sonatype.com/repository/maven-snapshots/org/dataverse/test/hello/0.0.5-SNAPSHOT/ you'll see "This maven2 hosted repository is not directly browseable at this URL. Please use the browse or HTML index views to inspect the contents of this repository." Sadly, the "browse" and "HTML index" links don't work, as noted in a [question](https://community.sonatype.com/t/this-maven2-group-repository-is-not-directly-browseable-at-this-url/8991) on the Sonatype Community forum. Below is a suggestion for confirming that the jar was uploaded properly, which is to use Maven to download the jar.

```
# be sure to update the version in the command below
mvn dependency:get -DremoteRepositories=https://central.sonatype.com/repository/maven-snapshots/ -Dartifact=org.dataverse.test:hello:0.0.5-SNAPSHOT
```

If the command above worked, the jar should now be in `~/.m2/repository/org/dataverse/test/hello/0.0.5-SNAPSHOT`. You can compare the checksum of the downloaded jar with the one in `target/hello-0.0.5-SNAPSHOT.jar` if you have any doubts about the snapshot being published properly.

Another approach would be to up a project that consumes snapshots (see https://central.sonatype.org/publish/publish-portal-snapshots/#consuming-snapshot-releases-for-your-project ) to verify that your snapshot was uploaded properly.


### Publish release from local environment

Now let's move on to publishing a real release from our local environment. We will be following a process similar to [.github/workflows/maven-release.yml](.github/workflows/maven-release.yml).

You'll need the `gpg` binary in the steps below. On Mac, you can install it with `brew install gnupg`.

You might want to run `gpg --list-keys` and `gpg --list-secret-keys --keyid-format=long` to see the keys you have right now. You may not have any, yet.

Import the private DATAVERSEBOT_GPG_KEY. Note that you will be prompted for DATAVERSEBOT_GPG_PASSWORD. Both are referenced in the GitHub Action above. You'll need to get the actual values from a member of the Dataverse core team.

```
gpg --import gdcc-key-till-2025-08-12.gpg
```

You should see output similar to this:

```
gpg: key BAFC446FC031F36B: public key "Dataverse Bot (Signing Bot for Dataverse and GDCC packages) <dataversebot@gdcc.io>" imported
gpg: key BAFC446FC031F36B: secret key imported
gpg: Total number processed: 1
gpg:               imported: 1
gpg:       secret keys read: 1
gpg:   secret keys imported: 1
```

Then, export environment variables with the key name and passphrase:

```
export DATAVERSEBOT_GPG_KEYNAME="BAFC446FC031F36B"
export DATAVERSEBOT_GPG_PASSWORD="REDACTED"
```

If you didn't just import the key, run `gpg --list-secret-keys --keyid-format=long` to find the right value for DATAVERSEBOT_GPG_KEYNAME, the GPG key ID. See also https://docs.github.com/en/authentication/managing-commit-signature-verification/checking-for-existing-gpg-keys

In the pom.xml, adjust the version to remove "-SNAPSHOT". For example, if the version number is "0.0.1-SNAPSHOT", make it "0.0.1".

Make a git commit for the pom.xml change.

Finally, publish the real release with this command:

`mvn -Prelease -Dgpg.keyname=$DATAVERSEBOT_GPG_KEYNAME -Dgpg.passphrase=$DATAVERSEBOT_GPG_PASSWORD deploy`

You'll see output like this:

```
[INFO] Uploaded bundle successfully, deployment name: hello, deploymentId: 52e47034-cc61-4cd4-8887-50a545a536ff. Deployment will publish automatically
[INFO] Waiting until Deployment 52e47034-cc61-4cd4-8887-50a545a536ff is validated
[INFO] Deployment 52e47034-cc61-4cd4-8887-50a545a536ff has been validated. To finish publishing visit https://central.sonatype.com/publishing/deployments
```

That last line is something of a lie. Publishing will happen automatically. Don't click the "Publish Component" button at https://central.sonatype.com/publishing/deployments no matter how tempting it may be. 😈

Give it some time, maybe half an hour, and you should see the version at https://repo1.maven.org/maven2/org/dataverse/test/hello/ and https://central.sonatype.com/artifact/org.dataverse.test/hello

When you publish using the GitHub Action, a tag is created and pushed automatically for you. We'll do these steps manually. But first, let's check what the tag messages look like with `git tag -n`. Now that we see the pattern, create the tag manually and push it, like in this example:

```
git tag -a hello-0.0.5 -m "[maven-release-plugin] copy for tag hello-0.0.5"
git push origin hello-0.0.5
```

### Publish snapshot from GitHub Actions

The GitHub Action at [.github/workflows/maven-snapshot.yml](.github/workflows/maven-snapshot.yml) is configured to publish snapshots automatically based on events.

At minimum, the following config means that merging pull requests or committing directly to main should trigger the publishing of a snapshot.

```yaml
on:
    push:
        branches:
            - main
```

### Publish release from GitHub Actions

The following commands rely on [.github/workflows/maven-release.yml](.github/workflows/maven-release.yml).

Before you begin, check that the version in the pom.xml ends with `-SNAPSHOT`. If it doesn't, someone might have made a real release manually (from a local environment rather than GitHub Actions, as described above), so you should add `-SNAPSHOT` before proceeding.

First run a clean:

```
mvn release:clean
```

Not that running the "prepare" command below will create two commits and a tag and push them to GitHub!

Then run a prepare:

```
mvn release:prepare
```

Note that the "prepare" step is interactive. Let's say the version is `0.0.2-SNAPSHOT` when you execute "prepare". You will be promped to remove `-SNAPSHOT` and set the release version to `0.2.2` like below. You are free to pick a different version, of course. Here's how the prompt looks:

```
What is the release version for "hello"? (org.dataverse.test:hello) 0.0.2: :
```

Next you will be asked what Git tag should be. It will default to `name-number` such as `hello-0.0.2` as shown below. Commonly on GitHub you'll see tags like `v0.0.2`, but we are sticking with the defaults offered by the release plugin. Here's the prompt:

```
What is SCM release tag or label for "hello"? (org.dataverse.test:hello) hello-0.0.2: :
```

Next you are asked what the next snapshot will be, such as `0.0.3-SNAPSHOT`, a new patch release. You can just accept this default.

```
What is the new development version for "hello"? (org.dataverse.test:hello) 0.0.3-SNAPSHOT: :
```

At this point you should see two new commits at https://github.com/gdcc/hello

You should also see a new tag at https://github.com/gdcc/hello/tags

Give it some time, maybe half an hour, and you should see the version at https://repo1.maven.org/maven2/org/dataverse/test/hello/ and https://central.sonatype.com/artifact/org.dataverse.test/hello
