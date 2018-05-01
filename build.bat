SET SKIPTEST=true

call mvn -Dmaven.test.skip=%SKIPTEST% package
call mvn -Dmaven.test.skip=%SKIPTEST% install

cd awss3examples 
call mvn package appassembler:assemble
cd ../
