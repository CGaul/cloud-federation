#Install shell-configs via git for current user:
mkdir scripts
cd scripts
git clone http://costa@lab.is-by.us:7990/stash/scm/shell/shell-configs.git
sleep 1
echo "Installing \"zsh-config\" locally..."
cd shell-configs/zsh-config
bash ./install-locally.sh
echo "zsh-config installation DONE."
echo "Change to zsh by \"chsh\" command and use \"/bin/zsh\" as new shell. Additionally, source ~/.zshrc"
echo
cd ../powerline-config
echo "Installing \"powerline-config\" locally..."
sudo bash ./install-locally.sh
echo "powerline-config installation DONE."
