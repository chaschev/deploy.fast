/**
 * Script is used to have multiple commands for the same packet sent to the server
 *
 * On sudo
 *   https://unix.stackexchange.com/questions/176997/sudo-as-another-user-with-their-environment
 *
 * How to do
 *
 * ScriptBuilder
 * CommandBuilder
 * LineBuilder(sudo, user, tty, dir)

 script {
  sudo = true
  dir = '.'
  abortOnError = false (default)

  command("service cassandra restart") {
    dir = '.'
    sudo = true
    user = 'vagrant'
    promptCallback =
    before = {}
    process = (console, myText) -> R
    processError = ...
  }

  commands() {
    line "tar xvfz $archive --directory=$dir"
    setRights dir, userRights      # translates into two lines
  }

 }

How to separate output from one another stdout+stderr

 echo "assholes don't like what we are trying to establish here $timestamp"
 echo "my timestamp bitch $? $PPID $timestamp"

 */