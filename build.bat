SET SKIPTEST=true

pushd ionics3
call mvn -Dmaven.test.skip=%SKIPTEST% package
call mvn -Dmaven.test.skip=%SKIPTEST% install
popd

pushd awss3examples
call mvn clean package appassembler:assemble
popd
