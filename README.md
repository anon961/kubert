# Build
```$xslt
git clone https://github.com/anon961/kubert
cd kubert && ./gradle build
```
# Usage
```$xslt
Usage: anon961.kubert.kubert [-hVx] [-n=<namespace>] [-o=<outputDir>]
                             [-t=<topName>] <dockerHubUser> <modelFile>
                             [<programArgs>...]
Deploy UML-RT models to Kubernetes clusters.
      <dockerHubUser>      DockerHub username to push containers to.
      <modelFile>          The UML-RT model to deploy.
      [<programArgs>...]   Program arguments.
  -h, --help               Show this help message and exit.
  -n, --namespace=<namespace>
                           Namespace to use for Kubernetes resources.
  -o, --output-dir=<outputDir>
                           Output directory for deployment files.
  -t, --top=<topName>      Name of the Top capsule for input models.
  -V, --version            Print version information and exit.
  -x, --show-ui            Show the user interface.
```