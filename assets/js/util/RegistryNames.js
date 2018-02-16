/*
  @author Sam Heutmaker [samheutmaker@gmail.com]
*/

export default function RegistryNames(includePuppet = false) {

  let RegistryNames = {
    'GCR': 'Google Container Registry',
    'ECR': 'EC2 Container Registry',
    'DOCKERHUB': 'DockerHub',
    // 'PRIVATE': 'Private Registry',
  };

  if (includePuppet) {
    RegistryNames['EUROPA'] = 'Puppet Container Registry';
  }

  return RegistryNames;
}
